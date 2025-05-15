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
    private static final String LEVEL1_HEADING_PATTERN = "^(?!.*\\.$)([1-9]\\d*)\\s+([A-ZА-Я]+(\\.\\s+[A-ZА-Я]+)*)";
    private static final String LEVEL2_TO_4_HEADING_PATTERN =
            "^(?!.*\\.$)([1-9]\\d*(\\.[1-9]\\d*){1,3})\\s+([A-ZА-Я][a-zа-я]*(\\.\\s+[A-ZА-Я][a-zа-я]*)*)";

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

    private int getHeadingLevel(XWPFParagraph para, CheckParams checkParams) {
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

                Pattern pattern = Pattern.compile(LEVEL2_TO_4_HEADING_PATTERN);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String[] numbers = matcher.group(1).split("\\.");
                    int actual = numbers.length;

                    if (actual != level) {
                        errorsList.addError(para.getText(), "errorIncorrectActualHeadingLevel");
                    }
                }
                if (!matcher.matches()) { // "^(?!.*\\.$)([1-9]\\d*(\\.[1-9]\\d*){1,3}).*"
                    if (text.endsWith(".")) {
                        errorsList.addError(text, "errorSubheadingHasPeriod");
                    } else {
                        errorsList.addError(text, "errorSubheadingInvalidFormat");
                    }
                }

                boolean isBold = false;
                for (XWPFRun run : para.getRuns()) {
                    if (run.isBold()) {
                        isBold = true;
                        break;
                    }
                }
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
