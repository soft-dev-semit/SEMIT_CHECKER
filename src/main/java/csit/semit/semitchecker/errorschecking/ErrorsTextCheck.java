package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;

import java.util.List;

public class ErrorsTextCheck implements IErrorsCheckable {
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkFontSettings(xwpfDocument, errorsList);
        checkParagraphSettings(xwpfDocument, errorsList);
        return errorsList;
    }

    private void checkFontSettings(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        int errorPositionParagraph = 0;
        for (XWPFParagraph paragraph : paragraphList) {
            String text = paragraph.getText().trim();
            if (paragraph.getStyle() != null || text.isEmpty()) {
                errorPositionParagraph++;
                continue;
            }
            for (XWPFRun run : paragraph.getRuns()) {
                String fontName = run.getFontName();
                if (fontName == null || !fontName.equals("Times New Roman")) {
                    errorsList.addError(errorPositionParagraph + " paragraph", "errorFontWrongName");
                }
                int fontSize = run.getFontSize();
                if (fontSize != 14 && fontSize != -1) { // -1 means default, we'll assume it's incorrect
                    errorsList.addError(errorPositionParagraph + " paragraph","errorFontWrongSize");
                }
                String color = run.getColor();
                if (color != null && !color.equalsIgnoreCase("000000")) {
                    errorsList.addError(errorPositionParagraph + " paragraph","errorFontWrongColor");
                }

            }
        }
    }

    private void checkParagraphSettings(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphList = xwpfDocument.getParagraphs();
        int errorPositionParagraph = 0;
        for (XWPFParagraph paragraph : paragraphList) {
            String text = paragraph.getText().trim();
            if (paragraph.getStyle() != null || text.isEmpty()) {
                errorPositionParagraph++;
                continue;
            }
            int firstLineIndent = paragraph.getIndentationFirstLine();
            if (firstLineIndent != 709) { // 1.25 cm = 709 twips (1 cm = 567 twips)
                errorsList.addError(errorPositionParagraph + " paragraph", "errorParagraphWrongIndent");
            }
            if (paragraph.getAlignment() != ParagraphAlignment.BOTH) {
                errorsList.addError(errorPositionParagraph + " paragraph", "errorParagraphWrongAlignment");
            }
            LineSpacingRule spacingRule = paragraph.getSpacingLineRule();
            int lineSpacing = (int) paragraph.getSpacingBetween();
            if (spacingRule != LineSpacingRule.AUTO || lineSpacing != 360) { // 1.5 * 240 = 360 twips
                errorsList.addError(errorPositionParagraph + " paragraph", "errorParagraphWrongLineSpacing");
            }
        }
    }
}
