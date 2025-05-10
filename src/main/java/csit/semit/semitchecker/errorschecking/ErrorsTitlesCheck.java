package csit.semit.semitchecker.errorschecking;

import csit.semit.semitchecker.serviceenums.StandardHeadings;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorsTitlesCheck implements IErrorsCheckable {
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkRequiredSections(xwpfDocument, checkParams, errorsList);
        checkHeadingSpacings(xwpfDocument, checkParams, errorsList);
        checkHeadingOrder(xwpfDocument, checkParams, errorsList);
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
        return para.getStyle() != null && isStandardHeading;
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
        if (standards.equals(foundStandards)) {
            return;
        }
        for (int i = 0; i < StandardHeadings.values().length; i++) {
            if (foundStandards.get(i) != standards.get(i)) {
                errorsList.addError(foundStandards.get(i), "errorStandardHeadingWrongPlace");
            }
        }
    }

    private void checkHeadingSpacings(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            String style = para.getStyle();
            if (style != null && style.matches("Heading[1-4]")) {
                if (i > 0 && !paragraphs.get(i - 1).getText().isEmpty()) {
                    errorsList.addError(para.getText(), "errorNoEmptyLineBeforeHeading");
                }
                if (i < paragraphs.size() - 1 && !paragraphs.get(i + 1).getText().isEmpty()) {
                    errorsList.addError(para.getText(), "errorNoEmptyLineAfterHeading");
                }
                if (style.equals("Heading1")) {
                    if (i > 0 && !paragraphs.get(i - 1).getText().isEmpty()) {
                        errorsList.addError(para.getText(), "errorHeading1NotOnNewPage");
                    }
                    int linesAfter = 0;
                    for (int j = i + 1; j < paragraphs.size(); j++) {
                        if (paragraphs.get(j).getText().isEmpty()) break;
                        linesAfter++;
                    }
                    if (linesAfter <= 1) {
                        errorsList.addError(para.getText(), "errorNotEnoughTextAfterHeading1");
                    }
                }
            }
        }
    }

    private void checkHeadingOrder(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        int[] currentLevel = new int[]{0, 0, 0, 0};
        for (XWPFParagraph para : paragraphs) {
            String style = para.getStyle();
            String[] headingStyles = {"H1", "H2", "H3", "H4"};
//            for (int i = 0; i < headingStyles.length; i++) {
//                if (style != null && style.matches(ResourceBundle.getBundle("resourcesbundles/docstyles/docswordstyles",
//                        checkParams.getLocaleWord()).getString(headingStyles[i]))) {
//
//                }
//            }
            if (style != null && style.matches("Heading[1-4]")) {
                int level = Integer.parseInt(style.replace("Heading", ""));
                String text = para.getText().trim();
                Pattern pattern = Pattern.compile("^(\\d+(\\.\\d+)*)\\s+.*");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String[] numbers = matcher.group(1).split("\\.");
                    for (int i = 0; i < numbers.length; i++) {
                        int expected = currentLevel[i] + (i == level - 1 ? 1 : 0);
                        int actual = Integer.parseInt(numbers[i]);
                        if (actual != expected) {
                            errorsList.addError(para.getText(), "errorIncorrectHeadingNumber");
                        }
                    }
                    currentLevel[level - 1]++;
                    for (int i = level; i < 4; i++) {
                        currentLevel[i] = 0;
                    }
                } else {
                    errorsList.addError(para.getText(), "errorMissingHeadingNumber");
                }
            }
        }
    }

    private void checkSectionFormatting(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        for (XWPFParagraph para : paragraphs) {
            if ("Heading1".equals(para.getStyle())) {
                String text = para.getText().trim();
                if (!text.equals(text.toUpperCase())) {
                    errorsList.addError(text, "errorHeading1NotUppercase");
                }
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
                if (!"CENTER".equals(alignment) && !"BOTH".equals(alignment)) {
                    errorsList.addError(text, "errorHeading1IncorrectAlignment");
                }
                if (text.matches(".*\\d+\\..*") || text.endsWith(".")) {
                    errorsList.addError(text, "errorHeading1HasPeriod");
                }
            }
        }
    }

    private void checkSubsectionFormatting(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        for (XWPFParagraph para : paragraphs) {
            String style = para.getStyle();
            if (style != null && style.matches("Heading[2-4]")) {
                String text = para.getText().trim();
                Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)*\\s+([A-Z][a-z\\s]+)$");
                if (!pattern.matcher(text).matches()) {
                    errorsList.addError(text, "errorSubheadingNotTitleCase");
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
                if (text.matches(".*\\d+\\..*") || text.endsWith(".")) {
                    errorsList.addError(text, "errorSubheadingHasPeriod");
                }
            }
        }
    }
}
