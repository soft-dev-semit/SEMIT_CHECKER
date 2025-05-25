package csit.semit.semitchecker.errorschecking;

import csit.semit.semitchecker.serviceenums.WordStyles;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ErrorsStyleCheck implements IErrorsCheckable {
    private static final double EPSILON = 1.0;

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkAllStyles(xwpfDocument, checkParams, errorsList);
        return errorsList;
    }

    private void checkStyle(XWPFStyle style, WordStyles wordStyle, ErrorsList errorsList) {
        CTStyle ctStyle = style.getCTStyle();
        if (ctStyle != null && ctStyle.isSetRPr()) {
            CTRPr rPr = ctStyle.getRPr();

            // Font Name
            String fontName = null;
            if (!rPr.getRFontsList().isEmpty()) {
                CTFonts fonts = rPr.getRFontsList().get(0);
                if (fonts != null) {
                    // Отримуємо шрифт для ASCII (основний шрифт для латиниці)
                    fontName = fonts.getAscii() != null ? fonts.getAscii() : null;
                    // Якщо ascii не задано, перевіряємо hAnsi
                    if (fontName == null && fonts.getHAnsi() != null) {
                        fontName = fonts.getHAnsi();
                    }
                }
            }
            if (fontName != wordStyle.getFontName()) {
                errorsList.addError(style.getName(), "errorWrongFontName");
            }

            // Font Color
            String fontColor = "auto";
            if (!rPr.getColorList().isEmpty()) {
                CTColor color = rPr.getColorList().get(0);
                fontColor = color.getVal() != null ? color.getVal().toString() : "auto";
            }
            if (fontColor != wordStyle.getFontColor()) {
                errorsList.addError(style.getName(), "errorWrongFontColor");
            }

            // Font Size (in points, divide half-points by 2)
            Integer fontSize = null;
            if (!rPr.getSzList().isEmpty()) {
                CTHpsMeasure sz = rPr.getSzList().get(0);
                if (sz.getVal() != null) {
                    // Cast Object to BigInteger
                    if (sz.getVal() instanceof BigInteger) {
                        fontSize = ((BigInteger) sz.getVal()).intValue() / 2;
                    }
                }
            }
            if (fontSize != wordStyle.getFontSize()) {
                errorsList.addError(style.getName(), "errorWrongFontSize");
            }

            // Bold
            Boolean isBold = false;
            if (!rPr.getBList().isEmpty()) {
                CTOnOff bold = rPr.getBList().get(0);
                Object val = bold.getVal();
                // Check if val represents "true" (can be "1" or "true" in string form)
                isBold = val != null && ("1".equals(val.toString()) || "true".equalsIgnoreCase(val.toString()));
            }
            if (isBold != wordStyle.getIsBold()) {
                errorsList.addError(style.getName(), "errorWrongBold");
            }

            // Italic
            Boolean isItalic = false;
            if (!rPr.getIList().isEmpty()) {
                CTOnOff italic = rPr.getIList().get(0);
                Object val = italic.getVal();
                isItalic = val != null && ("1".equals(val.toString()) || "true".equalsIgnoreCase(val.toString()));
            }
            if (isItalic != wordStyle.getIsItalic()) {
                errorsList.addError(style.getName(), "errorWrongItalic");
            }

            // Underline
            String underline = "NONE";
            if (!rPr.getUList().isEmpty() && rPr.getUList().get(0).getVal() != null) {
                underline = rPr.getUList().get(0).getVal().toString();
            }
            if (underline != wordStyle.getUnderline()) {
                errorsList.addError(style.getName(), "errorWrongUnderline");
            }
        }

        // Access paragraph properties (CTPPr) for alignment, indentation, and spacing
        if (ctStyle != null && ctStyle.isSetPPr()) {
            CTPPrGeneral pPr = ctStyle.getPPr();

            // Alignment
            String alignment = "LEFT";
            if (pPr.getJc() != null && pPr.getJc().getVal() != null) {
                alignment = pPr.getJc().getVal().toString();
            }
            if (alignment != wordStyle.getAlignment()) {
                errorsList.addError(style.getName(), "errorWrongAlignment");
            }

            // Indentation
            Integer indentationFirstLine = null;
            Integer indentationLeft = null;
            Integer indentationRight = null;
            if (pPr.getInd() != null) {
                if (pPr.getInd().getFirstLine() != null) {
                    Object firstLine = pPr.getInd().getFirstLine();
                    if (firstLine instanceof BigInteger) {
                        indentationFirstLine = ((BigInteger) firstLine).intValue();
                    }
                }
                if (pPr.getInd().getLeft() != null) {
                    Object left = pPr.getInd().getLeft();
                    if (left instanceof BigInteger) {
                        indentationLeft = ((BigInteger) left).intValue();
                    }
                }
                if (pPr.getInd().getRight() != null) {
                    Object right = pPr.getInd().getRight();
                    if (right instanceof BigInteger) {
                        indentationRight = ((BigInteger) right).intValue();
                    }
                }
            }
            if (indentationFirstLine != wordStyle.getIndentationFirstLine()) {
                errorsList.addError(style.getName(), "errorWrongIndentationFirstLine");
            }
            if (indentationLeft != wordStyle.getIndentationLeft()) {
                errorsList.addError(style.getName(), "errorWrongIndentationLeft");
            }
            if (indentationRight != wordStyle.getIndentationRight()) {
                errorsList.addError(style.getName(), "errorWrongIndentationRight");
            }

            // Spacing
            Integer spacingBetween = null;
            Integer spacingBefore = null;
            Integer spacingAfter = null;
            if (pPr.getSpacing() != null) {
                if (pPr.getSpacing().getLine() != null) {
                    Object line = pPr.getSpacing().getLine();
                    if (line instanceof BigInteger) {
                        spacingBetween = ((BigInteger) line).intValue();
                    }
                }
                if (pPr.getSpacing().getBefore() != null) {
                    Object before = pPr.getSpacing().getBefore();
                    if (before instanceof BigInteger) {
                        spacingBefore = ((BigInteger) before).intValue();
                    }
                }
                if (pPr.getSpacing().getAfter() != null) {
                    Object after = pPr.getSpacing().getAfter();
                    if (after instanceof BigInteger) {
                        spacingAfter = ((BigInteger) after).intValue();
                    }
                }
            }
            if (spacingBetween != wordStyle.getSpacingBetween()) {
                errorsList.addError(style.getName(), "errorWrongSpacingBetween");
            }
            if (spacingBefore != wordStyle.getSpacingBefore()) {
                errorsList.addError(style.getName(), "errorWrongSpacingBefore");
            }
            if (spacingAfter != wordStyle.getSpacingAfter()) {
                errorsList.addError(style.getName(), "errorWrongSpacingAfter");
            }
        }
    }

    private void checkAllStyles(XWPFDocument xwpfDocument, CheckParams checkParams, ErrorsList errorsList) {
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        List<String> usedStyles = new ArrayList<>();
        for (XWPFParagraph paragraph : paragraphs) {
            String style = switch (ErrorsTitlesCheck.getHeadingLevel(paragraph, checkParams)) {
                case 1 -> "H1";
                case 2 -> "H2";
                case 3 -> "H3";
                case 4 -> "H4";
                default -> "noheader";
            };
            if (!usedStyles.contains(style)) {
                usedStyles.add(style);
            }
        }
        for (String style : usedStyles) {
            XWPFStyle xwpfStyle = xwpfDocument.getStyles().getStyle(style);
            WordStyles styleToCompare = null;
            for (int i = 0; i < WordStyles.values().length; i++) {
                if (WordStyles.values()[i].getStyleName().equals(style)) {
                    styleToCompare = WordStyles.values()[i];
                    break;
                }
            }
            checkStyle(xwpfStyle, styleToCompare, errorsList);
        }
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
