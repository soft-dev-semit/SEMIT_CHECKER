package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;

import java.util.List;

public class ErrorsTextCheck implements IErrorsCheckable {
    private static final double EPSILON = 1.0;

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkFontSettings(xwpfDocument, errorsList);
        checkParagraphSettings(xwpfDocument, errorsList, checkParams);
        return errorsList;
    }

    private void checkFontSettings(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        for (int i = 0; i < paragraphList.size(); i++) {
            XWPFParagraph paragraph = paragraphList.get(i);
            String text = paragraph.getText().trim();
            if (text.isEmpty()) {
                continue;
            }
//            String errorPositionParagraph = paragraph.getStyle() != null ? paragraph.getText() : "Paragraph " + i;
            String errorPositionParagraph = paragraph.getText().length()>100? paragraph.getText().substring(0,100):paragraph.getText();
            String errorPositionParagraph = paragraph.getStyle() != null ? paragraph.getText() : "Paragraph " + i;
            for (XWPFRun run : paragraph.getRuns()) {
                String fontName = run.getFontName();
                if (fontName == null || !fontName.equals("Times New Roman")) {
                    errorsList.addError(errorPositionParagraph, "errorFontWrongName");
                }
                Double fontSize = run.getFontSizeAsDouble();
                double expectedFontSize = 14.0;
                if (fontSize != null && Math.abs(fontSize - expectedFontSize) > EPSILON) {
                    errorsList.addError(errorPositionParagraph, "errorFontWrongSize");
                }
                String color = run.getColor();
                if (color != null && !color.equalsIgnoreCase("000000") && !color.equalsIgnoreCase("auto")) {
                    errorsList.addError(errorPositionParagraph, "errorFontWrongColor");
                }

            }
        }
    }

    private void checkParagraphSettings(XWPFDocument xwpfDocument, ErrorsList errorsList, CheckParams checkParams) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        for (int i = 0; i < paragraphList.size(); i++) {
            XWPFParagraph paragraph = paragraphList.get(i);
            String text = paragraph.getText().trim();
            if (text.isEmpty()) {
                continue;
            }
//            String errorPositionParagraph = paragraph.getStyle() != null ? paragraph.getText() : "Paragraph " + i;
            String errorPositionParagraph = paragraph.getText().length()>100? paragraph.getText().substring(0,100):paragraph.getText();
            String errorPositionParagraph = paragraph.getStyle() != null ? paragraph.getText() : "Paragraph " + i;
            int firstLineIndent = paragraph.getIndentationFirstLine();
            if ((paragraph.getStyle() == null || ErrorsTitlesCheck.getHeadingLevel(paragraph, checkParams) > 1) &&
                    firstLineIndent != -1 && Math.abs(firstLineIndent - 709) > EPSILON) {
                errorsList.addError(errorPositionParagraph, "errorParagraphWrongIndent");
            }

            if (paragraph.getStyle() == null && ErrorsTitlesCheck.getHeadingLevel(paragraph, checkParams) == 0 &&
                    paragraph.getAlignment() != ParagraphAlignment.BOTH) {
                errorsList.addError(errorPositionParagraph, "errorParagraphWrongAlignment");
            }

            LineSpacingRule spacingRule = paragraph.getSpacingLineRule();
            double lineSpacing = paragraph.getSpacingBetween();
            if (lineSpacing != -1.0) {
                if (spacingRule == LineSpacingRule.AUTO && Math.abs(lineSpacing - 1.5) > EPSILON) {
                    errorsList.addError(errorPositionParagraph, "errorParagraphWrongLineSpacing");
                } else if (spacingRule == LineSpacingRule.EXACT && Math.abs(lineSpacing - 360) > EPSILON) {
                    errorsList.addError(errorPositionParagraph, "errorParagraphWrongLineSpacing");
                }
            }
        }
    }
}
