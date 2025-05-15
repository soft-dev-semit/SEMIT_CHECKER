package csit.semit.semitchecker.errorschecking;

import csit.semit.semitchecker.serviceenums.StandardHeadings;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;

public class ErrorsTitlesCheck implements IErrorsCheckable {
//    private static final String LEVEL1_HEADING_PATTERN = "^(?!.*\\.$)([1-9]\\d*)\\s+([A-ZА-Я]+(\\.\\s+[A-ZА-Я]+)*)";
    private static final String LEVEL1_HEADING_PATTERN = "^(?!.*\\.$)([1-9]\\d*)\\s+([A-ZА-Я[ЄЇІ]]+)(\\s+[A-ZА-Я[ЄЇІ]]+)*(\\.\\s+[A-ZА-Я[ЄЇІ]]+(\\s+[A-ZА-Я[ЄЇІ]]+)*)*";
//    private static final String LEVEL2_TO_4_HEADING_PATTERN =
//            "^(?!.*\\.$)([1-9]\\d*(\\.[1-9]\\d*){1,3})\\s([A-ZА-Я][A-ZА-Яa-zа-я]*)(\\s+[A-ZА-Яa-zа-я]+)*(\\.\\s+[A-ZА-Я][A-ZА-Яa-zа-я]*(\\s+[A-ZА-Яa-zа-я]+)*)*";
    private static final String LEVEL2_TO_4_HEADING_PATTERN =
        "^(?!.*\\.$)([1-9]\\d*(\\.[1-9]\\d*){1,3})\\s([A-ZА-Я[ЄЇІ]][A-ZА-Яa-zа-я[ЄЇІєїі']]*)(\\s+[A-ZА-Яa-zа-я[ЄЇІєїі']]+)*(\\.\\s+[A-ZА-Я[ЄЇІ]][A-ZА-Яa-zа-я[ЄЇІєїі']]*(\\s+[A-ZА-Яa-zа-я[ЄЇІєїі']]+)*)*";
    private static final String LEVEL2_TO_4_HEADING_PATTERN_NUMBERS = "^(?!.*\\.$)([1-9]\\d*(\\.[1-9]\\d*){1,3})\\s";
    private static final String LEVEL2_TO_4_HEADING_PATTERN_WORDS = "\\s([A-ZА-Я][A-ZА-Яa-zа-я]*)(\\s+[A-ZА-Яa-zа-я]*)*(\\.\\s+[A-ZА-Я][A-ZА-Яa-zа-я]*(\\s+[A-ZА-Яa-zа-я]*)*)*$";


    // Допоміжний клас для зберігання інформації про заголовки
    record HeadingInfo(int index, String text, boolean isStandard, String number) {}

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkRequiredSections(xwpfDocument, checkParams, errorsList);
        //checkHeadingOrder(xwpfDocument, checkParams, errorsList);
        checkSectionFormatting(xwpfDocument, checkParams, errorsList);
        checkSubsectionFormatting(xwpfDocument, checkParams, errorsList);
        return errorsList;
    }

    public static int getHeadingLevel(XWPFParagraph para, CheckParams checkParams) {
        String style = para.getStyle(); // Get the style, which might be null
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
            if (para.getStyle().equals(s)) {
                return level;
            }
            level++;
        }
        return 0;
    }

    private boolean isStandardHeading(XWPFParagraph para, CheckParams checkParams, ErrorsList errorsList) {
        boolean isStandardHeading = false;
        for (int i = 0; i < StandardHeadings.values().length; i++) {
            if (para.getText().equalsIgnoreCase(StandardHeadings.values()[i].getHeadingLocalized(checkParams))) {
                isStandardHeading = true;
                if (!para.getText().toUpperCase().equals(para.getText())) {
                    errorsList.addError(para.getText(), "errorStandardHeadingNotUppercase");
                }
                break;
            }
            if (para.getText().toUpperCase().startsWith(StandardHeadings.APPENDIX.getHeadingLocalized(checkParams))) {
                isStandardHeading = true;
                if (!para.getText().toUpperCase().equals(para.getText())) {
                    errorsList.addError(para.getText(), "errorStandardHeadingNotUppercase");
                }
                break;
            }
        }
        String heading1 = ResourceBundle.getBundle("resourcesbundles/docstyles/docswordstyles").getString("H1");
        return para.getStyle() == heading1 && isStandardHeading;
    }

    private void checkRequiredSections(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        List<String> foundStandards = new ArrayList<>();
        for (XWPFParagraph para : paragraphList) {
            if (isStandardHeading(para, checkParams, errorsList)) {
                foundStandards.add(para.getText().toUpperCase());
            }
        }
        List<String> standards = List.of(StandardHeadings.getAllHeadingsLocalized(checkParams));
        for (int i = 0; i < foundStandards.size(); i++) {
            if (foundStandards.get(i).toUpperCase().startsWith(StandardHeadings.APPENDIX.getHeadingLocalized(checkParams))) {
                continue;
            }
            if (i > standards.size() - 1 || !foundStandards.get(i).equals(standards.get(i))) {
                errorsList.addError(standards.get(i), "errorStandardHeadingWrongPlace");
            }
        }
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

            if (isStandardHeading(para, checkParams, errorsList)) {
                if (text.equals(StandardHeadings.INTRODUCTION.getHeadingLocalized(checkParams).toUpperCase())) {
                    introIndex = i;
                } else if (text.equals(StandardHeadings.CONCLUSIONS.getHeadingLocalized(checkParams).toUpperCase())) {
                    conclusionsIndex = i;
                }
                headings.add(new HeadingInfo(i, text, true, null));
            } else if (level != 0) {
                String number = extractHeadingNumber(para, level);
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

    private String extractHeadingNumber(XWPFParagraph para, int level) {
        String text = para.getText().trim();
        Pattern pattern = (level == 1) ? Pattern.compile(LEVEL1_HEADING_PATTERN) : Pattern.compile(LEVEL2_TO_4_HEADING_PATTERN);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1); // Номер заголовка (наприклад, "1" або "1.1.1")
        }
        return null;
    }

    private void checkStandardHeadingOrder(List<HeadingInfo> headings, int introIndex, int conclusionsIndex,
                                           List<String> standards, ErrorsList errorsList, CheckParams checkParams) {
        List<String> expectedBeforeIntroduction = standards.subList(0, standards.indexOf(StandardHeadings.INTRODUCTION.getHeadingLocalized(checkParams)));
        List<String> expectedAfterConclusions = standards.subList(standards.indexOf(StandardHeadings.CONCLUSIONS.getHeadingLocalized(checkParams)) + 1, standards.size());
        List<String> foundBeforeIntro = new ArrayList<>();
        List<String> foundAfterConclusions = new ArrayList<>();

        for (HeadingInfo heading : headings) {
            if (heading.isStandard()) {
                if (heading.index < introIndex || introIndex == -1) {
                    foundBeforeIntro.add(heading.text());
                } else if (heading.index > conclusionsIndex || conclusionsIndex == -1) {
                    foundAfterConclusions.add(heading.text());
                }
            }
        }

        // Перевірка порядку до ВСТУП
        for (int i = 0; i < foundBeforeIntro.size(); i++) {
            if (i >= expectedBeforeIntroduction.size() || !foundBeforeIntro.get(i).equals(expectedBeforeIntroduction.get(i))) {
                errorsList.addError(foundBeforeIntro.get(i), "errorStandardHeadingWrongPlaceBeforeIntro");
            }
        }

        // Перевірка порядку після ВИСНОВКИ (крім додатків)
        for (int i = 0; i < foundAfterConclusions.size(); i++) {
            String foundHeading = foundAfterConclusions.get(i);
            if (!foundHeading.startsWith(StandardHeadings.APPENDIX.getHeadingLocalized(checkParams).toUpperCase())) {
                if (i >= expectedAfterConclusions.size() || !foundHeading.equals(expectedAfterConclusions.get(i))) {
                    errorsList.addError(foundHeading, "errorStandardHeadingWrongPlaceAfterConclusions");
                }
            }
        }

        // Перевірка додатків
//        List<String> appendices = foundAfterConclusions.stream()
//                .filter(h -> h.startsWith(StandardHeadings.APPENDIX.getHeadingLocalized(checkParams).toUpperCase()))
//                .collect(Collectors.toList());
//        for (int i = 0; i < appendices.size(); i++) {
//            String expectedAppendix = (StandardHeadings.APPENDIX.getHeadingLocalized(checkParams) + " " + (char) ('А' + i)).toUpperCase();
//            if (!appendices.get(i).equals(expectedAppendix)) {
//                errorsList.addError(appendices.get(i), "errorAppendixWrongOrder");
//            }
//        }

        // Перевірка стандартних заголовків між ВСТУП і ВИСНОВКИ
        for (HeadingInfo heading : headings) {
            if (heading.isStandard() && heading.index > introIndex && heading.index < conclusionsIndex &&
                    !heading.text().equals(StandardHeadings.INTRODUCTION.getHeadingLocalized(checkParams).toUpperCase()) &&
                    !heading.text().equals(StandardHeadings.CONCLUSIONS.getHeadingLocalized(checkParams).toUpperCase())) {
                errorsList.addError(heading.text(), "errorStandardHeadingBetweenIntroAndConclusions");
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
            if (current.index < introIndex || current.index > conclusionsIndex) {
                errorsList.addError(current.text(), "errorNonStandardHeadingOutsideIntroAndConclusions");
                continue;
            }

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

        // Перевірка, чи поточний номер є продовженням попереднього
        int minLength = Math.min(prevParts.length, currParts.length);
        for (int i = 0; i < minLength; i++) {
            int prevValue = Integer.parseInt(prevParts[i]);
            int currValue = Integer.parseInt(currParts[i]);
            if (currValue < prevValue) {
                return false; // Номер не може зменшуватися на тому ж рівні
            } else if (currValue > prevValue) {
                // Новий рівень має починатися з 1 або продовжувати попередній рівень
                if (i < prevParts.length - 1 || currParts.length <= prevParts.length) {
                    return false; // Неправильне продовження (наприклад, 1.2 після 1.1.1)
                }
                if (currValue != prevValue + 1 && i == prevParts.length - 1) {
                    return false; // Наступний рівень має бути на 1 більше (наприклад, 1.2 після 1.1)
                }
                break;
            }
        }

        // Перевірка, чи поточний номер має більше рівнів, ніж попередній
        if (currParts.length > prevParts.length) {
            int lastPrev = Integer.parseInt(prevParts[prevParts.length - 1]);
            int firstNew = Integer.parseInt(currParts[prevParts.length]);
            return firstNew == 1; // Новий підрівень має починатися з 1 (наприклад, 1.1.1 після 1.1)
        }

        return true;
    }

    private void checkHeadingIntersection(List<HeadingInfo> headings, int introIndex, int conclusionsIndex,
                                          ErrorsList errorsList, CheckParams checkParams) {
        boolean inNonStandardSection = false;
        for (HeadingInfo heading : headings) {
            if (heading.isStandard()) {
                String text = heading.text();
                if (text.equals(StandardHeadings.INTRODUCTION.getHeadingLocalized(checkParams).toUpperCase())) {
                    inNonStandardSection = true;
                } else if (text.equals(StandardHeadings.CONCLUSIONS.getHeadingLocalized(checkParams).toUpperCase())) {
                    inNonStandardSection = false;
                } else if (inNonStandardSection && heading.index > introIndex && heading.index < conclusionsIndex) {
                    errorsList.addError(text, "errorStandardHeadingBetweenIntroAndConclusions");
                }
            } else if (heading.number() != null) {
                if (!inNonStandardSection && (introIndex != -1 && heading.index < introIndex) ||
                        (conclusionsIndex != -1 && heading.index > conclusionsIndex)) {
                    errorsList.addError(heading.text(), "errorNonStandardHeadingOutsideIntroAndConclusions");
                }
            }
        }
    }


    private void checkSectionFormatting(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            if (getHeadingLevel(para, checkParams) == 1) {
                if (i > 0) {
                    XWPFParagraph prevPara = paragraphs.get(i - 1);
                    boolean startsNewPage = false;
                    CTSectPr prevSectPr = prevPara.getCTPPr().getSectPr();
                    if (prevSectPr != null && prevSectPr.getType() != null) {
                        String breakType = prevSectPr.getType().getVal().toString();
                        if ("nextPage".equals(breakType)) {
                            startsNewPage = true;
                        }
                    }
                    if (!startsNewPage) {
                        errorsList.addError(para.getText(), "errorHeading1NotOnNewPage");
                        if (!prevPara.getText().isEmpty()) {
                            errorsList.addError(para.getText(), "errorNoEmptyLineBeforeHeading1");
                        }
                    }
                }

                if (i < paragraphs.size() - 1 && !paragraphs.get(i + 1).getText().isEmpty()) {
                    errorsList.addError(para.getText(), "errorNoEmptyLineAfterHeading1");
                }

                String text = para.getText().trim();

                boolean isBold = false;
                for (XWPFRun run : para.getRuns()) {
                    if (run.isBold()) {
                        isBold = true;
                        break;
                    }
                }
                if (!isBold) {
                    errorsList.addError(text, "errorHeading1NotBold");
                }
                String alignment = para.getAlignment().toString();
                if (!"CENTER".equals(alignment)) {
                    errorsList.addError(text, "errorHeading1IncorrectAlignment");
                }
                if (!isStandardHeading(para, checkParams, errorsList) && !text.matches(LEVEL1_HEADING_PATTERN)) {   // "^(?!.*\\.$)([1-9]\\d*).*"
                    if (text.endsWith(".")) {
                        errorsList.addError(text, "errorHeading1HasPeriod");
                    }
                    if (!text.equals(text.toUpperCase())) {
                        errorsList.addError(text, "errorHeading1NotUppercase");
                    }
                    else {
                        errorsList.addError(text, "errorHeading1InvalidFormat");
                    }
                }
            }
        }
    }

    private void checkSubsectionFormatting(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            String style = para.getStyle();
            int level = getHeadingLevel(para, checkParams);
            if (style != null && level >= 2 && level <=4) {

                if (level == 2) {
                    if (i > 0 && !paragraphs.get(i - 1).getText().isEmpty()) {
                        errorsList.addError(para.getText(), "errorNoEmptyLineBeforeHeading2");
                    }
                }

                String text = para.getText().trim();

                Pattern pattern = Pattern.compile(LEVEL2_TO_4_HEADING_PATTERN, Pattern.UNICODE_CHARACTER_CLASS);
                Matcher matcher = pattern.matcher(text);
                if (!matcher.matches()) {
                    System.out.println("Failed pattern for: " + text + " with pattern: " + LEVEL2_TO_4_HEADING_PATTERN);
                    errorsList.addError(text, "errorSubheadingInvalidFormat");
                }
                else {
                    String[] numbers = matcher.group(1).split("\\.");
                    int actualLevel = numbers.length;
                    if (actualLevel != level) {
                        errorsList.addError(para.getText(), "errorIncorrectActualHeadingLevel");
                    }
                }

                boolean isBold = para.getRuns().stream().anyMatch(XWPFRun::isBold);
                if (!isBold) {
                    errorsList.addError(text, "errorSubheadingNotBold");
                }
                if (!"BOTH".equals(para.getAlignment().toString())) {
                    errorsList.addError(text, "errorSubheadingIncorrectAlignment");
                }
            }
        }
    }
}
