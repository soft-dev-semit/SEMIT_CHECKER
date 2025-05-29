package csit.semit.semitchecker.errorschecking;

import csit.semit.semitchecker.serviceenums.StandardHeadings;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class ErrorsTitlesCheck implements IErrorsCheckable {
    private static final String HEADING_PATTERN = "^(?!.*\\.$)([1-9]\\d*(\\.[1-9]\\d*){0,3})\\s.+";
    private static final String WRONG_HEADING_PATTERN = "^((\\d+\\.)+)\\s.+";

    private static final int REQUIRED_SPACING_BEFORE_AFTER = 21 * 20; // 21 пт у twips (1 пт = 20 twips)

    // Допоміжний клас для зберігання інформації про заголовки
    record HeadingInfo(int index, String text, boolean isStandard, String number) {}

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkSectionFormatting(xwpfDocument, checkParams, errorsList);
        if (errorsList.getErrors().size() == 0) {
            checkHeadingOrder(xwpfDocument, checkParams, errorsList);
        }
        checkRequiredSections(xwpfDocument, checkParams, errorsList);
        return errorsList;
    }

    public static int getHeadingLevel(XWPFParagraph para, CheckParams checkParams) {
        String style = para.getStyle();
        if (style == null) {
            return 0;
        }

        ResourceBundle rb = ResourceBundle.getBundle("resourcesbundles/docstyles/docswordstyles", checkParams.getLocaleWord());
        String heading1 = rb.getString("H1");
        String heading2 = rb.getString("H2");
        String heading3 = rb.getString("H3");
        String heading4 = rb.getString("H4");

        String[] headingStyles = {heading1, heading2, heading3, heading4};
        int level = 1;
        for (String s : headingStyles) {
            if (style.equals(s)) {
                return level;
            }
            level++;
        }
        return 0;
    }

    private boolean isStandardHeading(XWPFParagraph para, CheckParams checkParams) {
        boolean isStandardHeading = false;
        String text = para.getText().trim();

        // Перевіряємо чи це стандартний заголовок (ВСТУП, ВИСНОВКИ тощо)
        for (StandardHeadings heading : StandardHeadings.values()) {
            String localizedHeading = heading.getHeadingLocalized(checkParams);

            // Порівнюємо без урахування регістру
            if (text.equalsIgnoreCase(localizedHeading)) {
                isStandardHeading = true;
                break;
            }

            // Спеціальна перевірка для ДОДАТКІВ
            if (text.toUpperCase().startsWith(StandardHeadings.APPENDIX.getHeadingLocalized(checkParams).toUpperCase())) {
                isStandardHeading = true;
                break;
            }
        }

        // Повертаємо true тільки якщо це стандартний заголовок і має стиль Heading 1
        ResourceBundle rb = ResourceBundle.getBundle("resourcesbundles/docstyles/docswordstyles", checkParams.getLocaleWord());
        String heading1 = rb.getString("H1");
        return para.getStyle() != null && para.getStyle().equals(heading1) && isStandardHeading;
    }

    private void checkRequiredSections(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        List<String> foundStandards = new ArrayList<>();
        String contentHeading = StandardHeadings.CONTENTS.getHeadingLocalized(checkParams).toUpperCase();
        String appendixHeadingPrefix = StandardHeadings.APPENDIX.getHeadingLocalized(checkParams).toUpperCase();

        for (XWPFParagraph para : paragraphList) {
            if (isStandardHeading(para, checkParams)) {
                foundStandards.add(para.getText().toUpperCase());
            }
        }

        List<String> standards = List.of(StandardHeadings.getAllHeadingsLocalized(checkParams));
//        System.out.println("Expected standard headings: " + standards);
//        System.out.println("Found standard headings: " + foundStandards);

        // Виключаємо ЗМІСТ і ДОДАТОК зі списку обов’язкових заголовків
        List<String> mandatoryStandards = standards.stream()
                .filter(h -> !h.toUpperCase().equals(contentHeading) && !h.toUpperCase().startsWith(appendixHeadingPrefix))
                .collect(Collectors.toList());

        // Перевіряємо лише наявність усіх обов’язкових заголовків
        for (String standard : mandatoryStandards) {
            if (!foundStandards.contains(standard)) {
                errorsList.addError(standard, "errorStandardHeadingWrongPlace");
            }
        }
    }

    private void checkSectionFormatting(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        for (int i = 0; i < paragraphList.size(); i++) {
            XWPFParagraph para = paragraphList.get(i);
            int level = getHeadingLevel(para, checkParams);
            String text = para.getText().trim();
            if (level == 1) {
                // Перевірка розриву розділу (з нової сторінки)
                if (i > 0) {
                    boolean startsNewPage = checkSectionBreak(para, paragraphList, i, xwpfDocument);
                    if (!startsNewPage) {
                        errorsList.addError(text, "errorHeading1NotOnNewPage");
                        // Перевірка інтервалу перед заголовком з діагностикою, якщо немає розриву розділу
                        XWPFParagraph prevPara = paragraphList.get(i - 1);
                        if (prevPara != null) {
                            if (prevPara.getCTP().getPPr() != null && prevPara.getCTP().getPPr().getSpacing() != null) {
                                CTSpacing spacing = prevPara.getCTP().getPPr().getSpacing();
                                Object afterObj = spacing.getAfter();
                                BigInteger after = (afterObj instanceof BigInteger) ? (BigInteger) afterObj : null;
                            }
                            if (!hasRequiredSpacingBefore(prevPara)) {
                                errorsList.addError(text, "errorNoEmptyLineBeforeHeading1");
                            }
                        }
                    }
                }

                // Перевірка інтервалу після заголовка
                if (i < paragraphList.size() - 1) {
                    if (!hasRequiredSpacingAfter(para, paragraphList.get(i + 1))) {
                        errorsList.addError(text, "errorNoEmptyLineAfterHeading1");
                    }
                }


            }

            if (level == 2) {
                XWPFParagraph prevPara = paragraphList.get(i - 1);
                if (prevPara.getCTP().getPPr() != null && prevPara.getCTP().getPPr().getSpacing() != null) {
                    CTSpacing spacing = prevPara.getCTP().getPPr().getSpacing();
                    Object afterObj = spacing.getAfter();
                    BigInteger after = (afterObj instanceof BigInteger) ? (BigInteger) afterObj : null;
                }
                if (!hasRequiredSpacingBefore(prevPara)) {
                    errorsList.addError(text, "errorNoEmptyLineBeforeHeading2");
                }
            }

            if (level != 0) {
                // Перевірка формату заголовка
                if (!isStandardHeading(para, checkParams)) {
                    try {
                        Pattern pattern = Pattern.compile(HEADING_PATTERN);
                        Matcher matcher = pattern.matcher(text);
                        if (!matcher.matches()) {
                            boolean errorFlag = false;
                            if (text.endsWith(".")) {
                                errorFlag = true;
                                errorsList.addError(text, "errorHeadingHasPeriodInTheEnd");
                            }

                            Pattern wrongPattern = Pattern.compile(WRONG_HEADING_PATTERN);
                            Matcher wrongMatcher = wrongPattern.matcher(text);
                            if (wrongMatcher.matches() && wrongMatcher.group(1).endsWith(".")) {
                                errorFlag = true;
                                errorsList.addError(text, "errorHeadingHasPeriodAfterNumber");
                            }
                            if (!errorFlag) {
                                errorsList.addError(text, "errorHeadingInvalidFormat");
                            }
                        }
                        else {
                            String[] numbers = matcher.group(1).split("\\.");
                            int actualLevel = numbers.length;
                            if (actualLevel != level) {
                                errorsList.addError(text.toUpperCase(), "errorIncorrectActualHeadingLevel");
                            }
                        }
                    } catch (Exception e) {
                        errorsList.addError(text, "errorHeadingInvalidFormat");
                    }
                }
            }
        }
    }

    // Допоміжний метод для перевірки розриву сторінки
    private boolean checkSectionBreak(XWPFParagraph para, List<XWPFParagraph> paragraphs, int index, XWPFDocument document) {
        if (index <= 0) {
            return true; // Перший абзац завжди вважаємо на новій сторінці
        }

        // Перевіряємо наявність розриву сторінки у попередніх абзацах
        XWPFParagraph prevPara = paragraphs.get(index - 1);

        // 1. Перевіряємо атрибути секції
        if (prevPara.getCTP() != null && prevPara.getCTP().getPPr() != null) {
            if (prevPara.getCTP().getPPr().isSetSectPr()) {
                // Є секція з розривом сторінки
                return true;
            }
        }

        // 2. Перевіряємо розриви сторінок у самих runs
        for (XWPFRun run : prevPara.getRuns()) {
            // Перевіряємо наявність break character (код 12 або '\f')
            String text = run.getText(0);
            if (text != null && text.contains("\f")) {
                return true;
            }

            // Перевіряємо CTR на наявність br елементів та їх типів
            if (run.getCTR() != null) {
                // Отримуємо всі елементи <br>
                CTBr[] breakElements =
                        run.getCTR().getBrArray();

                if (breakElements != null && breakElements.length > 0) {
                    for (CTBr br : breakElements) {
                        // Перевіряємо атрибут type зі значенням "page"
                        if (br.isSetType() && "page".equals(br.getType().toString())) {
                            return true;
                        }
                    }
                }

                // Перевіряємо наявність lastRenderedPageBreak через XML структуру
                // Оскільки немає прямого доступу до lastRenderedPageBreak, використовуємо DOM Node
                XmlObject xmlObj = run.getCTR();
                Node node = xmlObj.getDomNode();
                NodeList childNodes = node.getChildNodes();

                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    if ("lastRenderedPageBreak".equals(childNode.getLocalName())) {
                        return true;
                    }
                }
            }
        }

        // 3. Перевіряємо також поточний абзац
        if (para.getCTP() != null && para.getCTP().getPPr() != null) {
            if (para.getCTP().getPPr().isSetPageBreakBefore()) {
                return true;
            }
        }

        // 4. Перевіряємо стиль для page-break-before у поточному абзаці
        if (para.getStyle() != null) {
            XWPFStyle style = para.getDocument().getStyles().getStyle(para.getStyle());
            if (style != null && style.getCTStyle() != null && style.getCTStyle().getPPr() != null) {
                if (style.getCTStyle().getPPr().isSetPageBreakBefore()) {
                    return true;
                }
            }
        }

        // 5. Перевіряємо розриви сторінок у попередніх абзацах (для випадків з <br> без явного типу)
        for (int i = index - 1; i >= 0; i--) {
            XWPFParagraph p = paragraphs.get(i);

            // Якщо абзац з текстом, далі не шукаємо
            if (!p.getText().trim().isEmpty() && i < index - 1) {
                break;
            }

            for (XWPFRun r : p.getRuns()) {
                if (r.getCTR() != null) {
                    // Перевіряємо наявність будь-яких розривів типу <br>
                    CTBr[] breakElements =
                            r.getCTR().getBrArray();

                    if (breakElements != null && breakElements.length > 0) {
                        return true;
                    }

                    // Перевіряємо DOM структуру на наявність lastRenderedPageBreak
                    XmlObject xmlObj = r.getCTR();
                    Node node = xmlObj.getDomNode();
                    NodeList childNodes = node.getChildNodes();

                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node childNode = childNodes.item(j);
                        if ("lastRenderedPageBreak".equals(childNode.getLocalName())) {
                            return true;
                        }
                    }
                }
            }
        }

        // 6. Перевірка на розрив секції в документі
        if (document != null && document.getDocument() != null &&
                document.getDocument().getBody() != null &&
                document.getDocument().getBody().isSetSectPr()) {

            // Перевіряємо document body section properties
            CTSectPr sectPr =
                    document.getDocument().getBody().getSectPr();

            if (sectPr != null && sectPr.isSetType()) {
                CTSectType sectType = sectPr.getType();
                if (sectType != null && sectType.isSetVal() &&
                        "nextPage".equals(sectType.getVal().toString())) {
                    return true;
                }
            }
        }

        return false;
    }

    // Перевірка, чи є необхідний інтервал перед абзацом (21 пт у попереднього абзацу або порожній рядок)
    private boolean hasRequiredSpacingBefore(XWPFParagraph prevPara) {
        // Перевірка, чи попередній абзац порожній
        if (prevPara.getText().isEmpty()) {
            return true;
        }

        // Перевірка інтервалу після попереднього абзацу
        if (prevPara.getCTP().getPPr() != null && prevPara.getCTP().getPPr().getSpacing() != null) {
            CTSpacing spacing = prevPara.getCTP().getPPr().getSpacing();
            Object afterObj = spacing.getAfter();
            BigInteger after = (afterObj instanceof BigInteger) ? (BigInteger) afterObj : null;
            if (after != null && after.intValue() >= REQUIRED_SPACING_BEFORE_AFTER) {
                return true;
            }
        }

        // Перевірка інтервалу через стиль
        if (prevPara.getStyle() != null) {
            XWPFStyle style = prevPara.getDocument().getStyles().getStyle(prevPara.getStyle());
            if (style != null && style.getCTStyle() != null) {
                CTStyle ctStyle = style.getCTStyle();
                if (ctStyle.getPPr() != null && ctStyle.getPPr().getSpacing() != null) {
                    CTSpacing spacing = ctStyle.getPPr().getSpacing();
                    Object afterObj = spacing.getAfter();
                    BigInteger after = (afterObj instanceof BigInteger) ? (BigInteger) afterObj : null;
                    if (after != null && after.intValue() >= REQUIRED_SPACING_BEFORE_AFTER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Перевірка, чи є необхідний інтервал після абзацу (21 пт або порожній наступний рядок)
    private boolean hasRequiredSpacingAfter(XWPFParagraph para, XWPFParagraph nextPara) {
        // Перевірка, чи наступний абзац порожній
        if (nextPara.getText().isEmpty()) {
            return true;
        }

        // Перевірка інтервалу після поточного абзацу
        if (para.getCTP().getPPr() != null && para.getCTP().getPPr().getSpacing() != null) {
            CTSpacing spacing = para.getCTP().getPPr().getSpacing();
            Object afterObj = spacing.getAfter();
            BigInteger after = (afterObj instanceof BigInteger) ? (BigInteger) afterObj : null;
            if (after != null && after.intValue() >= REQUIRED_SPACING_BEFORE_AFTER) {
                return true;
            }
        }

        // Перевірка інтервалу через стиль
        if (para.getStyle() != null) {
            XWPFStyle style = para.getDocument().getStyles().getStyle(para.getStyle());
            if (style != null && style.getCTStyle() != null) {
                CTStyle ctStyle = style.getCTStyle();
                if (ctStyle.getPPr() != null && ctStyle.getPPr().getSpacing() != null) {
                    CTSpacing spacing = ctStyle.getPPr().getSpacing();
                    Object afterObj = spacing.getAfter();
                    BigInteger after = (afterObj instanceof BigInteger) ? (BigInteger) afterObj : null;
                    if (after != null && after.intValue() >= REQUIRED_SPACING_BEFORE_AFTER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Допоміжний метод для перевірки жирності з урахуванням стилю
    private boolean isParagraphBold(XWPFParagraph para) {
        // Перевіряємо пряме форматування
        boolean isDirectlyBold = !para.getRuns().isEmpty();
        for (XWPFRun run : para.getRuns()) {
            if (run.getText(0) == null || run.getText(0).trim().isEmpty()) {
                continue; // Пропускаємо порожні run
            }
            isDirectlyBold &= run.isBold();
        }
        if (isDirectlyBold) {
            return true;
        }

        // Перевіряємо стиль
        if (para.getStyle() != null) {
            XWPFStyle xwpfStyle = para.getDocument().getStyles().getStyle(para.getStyle());
            if (xwpfStyle != null) {
                CTStyle style = xwpfStyle.getCTStyle();
                if (style != null && style.getRPr() != null) {
                    CTOnOff[] boldArray = style.getRPr().getBArray();
                    if (boldArray != null && boldArray.length > 0) {
                        Object val = boldArray[0].getVal();
                        if (val == null ||
                                "true".equals(val.toString()) ||
                                "1".equals(val.toString())) {
                            return true;
                        }
                    }

                    // Якщо стиль "Заголовок 1" або "Заголовок 2-4", припускаємо жирність за замовчуванням
                    ResourceBundle rb = ResourceBundle.getBundle("resourcesbundles/docstyles/docswordstyles");
                    String heading1 = rb.getString("H1");
                    String heading2 = rb.getString("H2");
                    String heading3 = rb.getString("H3");
                    String heading4 = rb.getString("H4");
                    if (para.getStyle().equals(heading1) ||
                            para.getStyle().equals(heading2) ||
                            para.getStyle().equals(heading3) ||
                            para.getStyle().equals(heading4)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Допоміжний метод для визначення ефективного вирівнювання
    private String getEffectiveAlignment(XWPFParagraph para, CheckParams checkParams) {
        // Перевіряємо пряме вирівнювання абзацу
        String directAlignment = para.getAlignment().toString();
        if (!"LEFT".equals(directAlignment)) {
            return directAlignment;
        }

        // Якщо пряме вирівнювання не задано, перевіряємо стиль
        if (para.getStyle() != null) {
            XWPFStyle xwpfStyle = para.getDocument().getStyles().getStyle(para.getStyle());
            if (xwpfStyle != null) {
                CTStyle style = xwpfStyle.getCTStyle();
                if (style != null && style.getPPr() != null && style.getPPr().getJc() != null) {
                    String styleAlignment = style.getPPr().getJc().getVal().toString();
                    switch (styleAlignment.toLowerCase()) {
                        case "center":
                            return "CENTER";
                        case "both":
                            return "BOTH";
                        case "left":
                            return "LEFT";
                        case "right":
                            return "RIGHT";
                        default:
                            return "LEFT";
                    }
                }
                // Якщо це підрозділ (H2-H4), припускаємо виправлене вирівнювання за замовчуванням
                int level = getHeadingLevel(para, checkParams);
                if (level >= 2 && level <= 4) {
                    return "BOTH";
                }
            }
        }

        return "LEFT";
    }

    private void checkHeadingOrder(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        List<String> standards = List.of(StandardHeadings.getAllHeadingsLocalized(checkParams));
        int introIndex = -1;
        int conclusionsIndex = -1;
        List<HeadingInfo> headings = new ArrayList<>();

        // Збір інформації про всі заголовки
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            String text = para.getText().trim().toUpperCase();
            int level = getHeadingLevel(para, checkParams);

            // Перевірка стандартних заголовків (з урахуванням ЗМІСТ без стилю Heading 1)
            if (isStandardHeading(para, checkParams)) {
                if (text.equals(StandardHeadings.INTRODUCTION.getHeadingLocalized(checkParams).toUpperCase())) {
                    introIndex = i;
                } else if (text.equals(StandardHeadings.CONCLUSIONS.getHeadingLocalized(checkParams).toUpperCase())) {
                    conclusionsIndex = i;
                }
                headings.add(new HeadingInfo(i, text, true, null));
            } else if (level != 0) {

                //System.out.println("Non-standard heading: " + para.getText().trim());
                Pattern pattern = Pattern.compile(HEADING_PATTERN);
                Matcher matcher = pattern.matcher(para.getText().trim());
                String number = null;
                if (matcher.find()) {
                    number = matcher.group(1); // Номер заголовка (наприклад, "1" або "1.1.1")
                }
                if (number != null) {
                    headings.add(new HeadingInfo(i, text, false, number));
                }
            }
        }

        // 1. Перевірка порядку стандартних заголовків
        checkStandardHeadingOrder(headings, introIndex, conclusionsIndex, standards, errorsList, checkParams);

        // 2. Перевірка порядку нестандартних заголовків
        checkNonStandardHeadingOrder(headings, introIndex, conclusionsIndex, errorsList);

        // 3. Перевірка перехрещення
        checkHeadingIntersection(headings, introIndex, conclusionsIndex, errorsList, checkParams);
    }

    private void checkStandardHeadingOrder(List<HeadingInfo> headings, int introIndex, int conclusionsIndex,
                                           List<String> standards, ErrorsList errorsList, CheckParams checkParams) {
        String contentHeading = StandardHeadings.CONTENTS.getHeadingLocalized(checkParams).toUpperCase();
        String appendixHeadingPrefix = StandardHeadings.APPENDIX.getHeadingLocalized(checkParams).toUpperCase();

        // Виключаємо ЗМІСТ і ДОДАТОК зі списку обов’язкових стандартних заголовків
        List<String> mandatoryStandards = standards.stream()
                .filter(h -> !h.toUpperCase().equals(contentHeading) && !h.toUpperCase().startsWith(appendixHeadingPrefix))
                .collect(Collectors.toList());

        // Збираємо знайдені стандартні заголовки в порядку їх появи
        List<String> foundStandards = headings.stream()
                .filter(HeadingInfo::isStandard)
                .map(h -> h.text().toUpperCase())
                .filter(h -> !h.equals(contentHeading) && !h.startsWith(appendixHeadingPrefix))
                .collect(Collectors.toList());

        // Перевірка порядку стандартних заголовків
        for (int i = 0; i < foundStandards.size(); i++) {
            if (!foundStandards.get(i).equals(mandatoryStandards.get(i))) {
                errorsList.addError(foundStandards.get(i), "errorStandardHeadingWrongPlace");
            }
        }

        // Перевірка додатків
        List<String> appendices = headings.stream()
                .filter(h -> h.isStandard() && h.text().startsWith(appendixHeadingPrefix))
                .map(HeadingInfo::text)
                .collect(Collectors.toList());

        for (int i = 0; i < appendices.size(); i++) {
            final int finalIndex = i;
            // Формуємо очікувану назву додатка залежно від локалізації
            String appendixLetter = checkParams.getLocaleDoc() == Locale.ENGLISH ?
                    String.valueOf((char) ('A' + finalIndex)) :
                    String.valueOf((char) ('А' + finalIndex));
            String expectedAppendix = (StandardHeadings.APPENDIX.getHeadingLocalized(checkParams) + " " + appendixLetter).toUpperCase();
            if (!appendices.get(finalIndex).equals(expectedAppendix)) {
                errorsList.addError(appendices.get(finalIndex), "errorAppendixWrongOrder");
            }
            HeadingInfo appendixHeading = headings.stream()
                    .filter(h -> h.text().equals(appendices.get(finalIndex)))
                    .findFirst()
                    .orElse(null);
            if (appendixHeading != null && conclusionsIndex != -1 && appendixHeading.index() < conclusionsIndex) {
                errorsList.addError(appendices.get(finalIndex), "errorAppendixBeforeConclusions");
            }
        }
    }

    private void checkNonStandardHeadingOrder(List<HeadingInfo> headings, int introIndex, int conclusionsIndex,
                                              ErrorsList errorsList) {
        List<HeadingInfo> nonStandardHeadings = headings.stream()
                .filter(h -> !h.isStandard() && h.number() != null)
                .sorted(Comparator.comparingInt(HeadingInfo::index))
                .collect(Collectors.toList());

        for (int i = 0; i < nonStandardHeadings.size(); i++) {
            HeadingInfo current = nonStandardHeadings.get(i);
            // Нестандартні заголовки мають бути між ВСТУП і ВИСНОВКИ
            if (introIndex != -1 && conclusionsIndex != -1 && (current.index() < introIndex || current.index() > conclusionsIndex)) {
                errorsList.addError(current.text(), "errorNonStandardHeadingOutsideIntroAndConclusions");
                continue;
            }

            // Перевірка послідовності номерів
            if (i > 0) {
                HeadingInfo previous = nonStandardHeadings.get(i - 1);
                if (!isValidHeadingSequence(previous.number(), current.number())) {
                    errorsList.addError(current.text(), "errorNonStandardHeadingWrongOrder");
                }
            }
        }
    }

    private boolean isValidHeadingSequence(String prevNumber, String currNumber) {
        String[] prevParts = prevNumber.split("\\.");
        String[] currParts = currNumber.split("\\.");

        // Якщо поточний заголовок є прямим продовженням попереднього на тому ж рівні (наприклад, 2.1.1 -> 2.1.2)
        if (prevParts.length == currParts.length) {
            for (int i = 0; i < prevParts.length - 1; i++) {
                if (!prevParts[i].equals(currParts[i])) {
                    return false; // Префікс не збігається (наприклад, 2.1.1 -> 2.2.1)
                }
            }
            int prevLast = Integer.parseInt(prevParts[prevParts.length - 1]);
            int currLast = Integer.parseInt(currParts[currParts.length - 1]);
            return currLast == prevLast + 1; // Поточний номер має бути на 1 більшим
        }

        // Якщо поточний заголовок є підрівнем попереднього (наприклад, 2.1 -> 2.1.1 або 2 -> 2.1)
        if (currParts.length == prevParts.length + 1) {
            for (int i = 0; i < prevParts.length; i++) {
                if (!prevParts[i].equals(currParts[i])) {
                    return false; // Префікс не збігається
                }
            }
            return currParts[currParts.length - 1].equals("1"); // Новий підрівень має починатися з 1
        }

        // Якщо поточний заголовок є вищим рівнем або новим розділом (наприклад, 2.1.3 -> 2.2 або 2.3.3 -> 3 або 2.3.3.3 -> 3 або 2.3.3.3 -> 2.4)
        if (currParts.length < prevParts.length) {
            int numberOfFirstValuableDigits = currParts.length;

            // Перевіряємо, чи префікс коректний
            for (int i = 0; i < numberOfFirstValuableDigits - 1; i++) {
                if (!prevParts[i].equals(currParts[i])) {
                    return false; // Префікс не збігається
                }
            }

            int prevLast = Integer.parseInt(prevParts[numberOfFirstValuableDigits - 1]);
            int currLast = Integer.parseInt(currParts[numberOfFirstValuableDigits - 1]);
            return currLast == prevLast + 1; // Новий номер має бути на 1 більшим (наприклад, 2.3.3 -> 3)
        }

        return false; // Інші випадки некоректні
    }

    private void checkHeadingIntersection(List<HeadingInfo> headings, int introIndex, int conclusionsIndex,
                                          ErrorsList errorsList, CheckParams checkParams) {
        String contentHeading = StandardHeadings.CONTENTS.getHeadingLocalized(checkParams).toUpperCase();
        String appendixHeadingPrefix = StandardHeadings.APPENDIX.getHeadingLocalized(checkParams).toUpperCase();
        boolean inNonStandardSection = false;

        for (HeadingInfo heading : headings) {
            if (heading.isStandard()) {
                String text = heading.text();
                if (text.equals(StandardHeadings.INTRODUCTION.getHeadingLocalized(checkParams).toUpperCase())) {
                    inNonStandardSection = true;
                } else if (text.equals(StandardHeadings.CONCLUSIONS.getHeadingLocalized(checkParams).toUpperCase())) {
                    inNonStandardSection = false;
                } else if (inNonStandardSection && heading.index > introIndex && heading.index < conclusionsIndex) {
                    // Пропускаємо ЗМІСТ і ДОДАТОК
                    if (!text.equals(contentHeading) && !text.startsWith(appendixHeadingPrefix)) {
                        errorsList.addError(text, "errorStandardHeadingBetweenIntroAndConclusions");
                    }
                }
            } else if (heading.number() != null) {
                if (!inNonStandardSection && (introIndex != -1 && heading.index < introIndex) ||
                        (conclusionsIndex != -1 && heading.index > conclusionsIndex)) {
                    errorsList.addError(heading.text(), "errorNonStandardHeadingOutsideIntroAndConclusions");
                }
            }
        }
    }
}
