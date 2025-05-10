package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

public class ErrorsLayoutCheck implements IErrorsCheckable {
    private static final double TWENTIETHS_PER_MM = 56.692; // 1 мм через одиницю виміру у Word
    private static final int A4_WIDTH_MM = 210;
    private static final int A4_HEIGHT_MM = 297;
    private static final int TOP_BOTTOM_MARGIN_MM = 20; // ліворуч, зверху, знизу
    private static final int MIN_LEFT_MARGIN_MM = 20;
    private static final int MAX_LEFT_MARGIN_MM = 35;
    private static final int MIN_RIGHT_MARGIN_MM = 10; // праворуч
    private static final int MAX_RIGHT_MARGIN_MM = 15;

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkA4Fromat(xwpfDocument, errorsList);
        checkPageMargins(xwpfDocument, errorsList);
        //checkPageNumbering(xwpfDocument, errorsList);
        return errorsList;
    }

    private void checkA4Fromat(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        CTSectPr sectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (sectPr != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            if (pgSz != null) {
                int width = ((Number) pgSz.getW()).intValue();
                int height = ((Number) pgSz.getH()).intValue();
                if (width != (int) (A4_WIDTH_MM * TWENTIETHS_PER_MM) || height != (int) (A4_HEIGHT_MM * TWENTIETHS_PER_MM)) {
                    errorsList.addError("0", "errorPageFormatIncorrect");
                }
            }
        }
    }

    private void checkPageMargins(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        CTSectPr sectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (sectPr != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            if (pgSz != null) {
                CTPageMar pgMar = sectPr.getPgMar();
                if (pgMar != null) {
                    int left = ((Number) pgMar.getLeft()).intValue();
                    int right = ((Number) pgMar.getRight()).intValue();
                    int top = ((Number) pgMar.getTop()).intValue();
                    int bottom = ((Number) pgMar.getBottom()).intValue();
                    int topBottom = (int) (TOP_BOTTOM_MARGIN_MM * TWENTIETHS_PER_MM);
                    int minLeft = (int) (MIN_LEFT_MARGIN_MM * TWENTIETHS_PER_MM);
                    int maxLeft = (int) (MAX_LEFT_MARGIN_MM * TWENTIETHS_PER_MM);
                    int minRight = (int) (MIN_RIGHT_MARGIN_MM * TWENTIETHS_PER_MM);
                    int maxRight = (int) (MAX_RIGHT_MARGIN_MM * TWENTIETHS_PER_MM);

                    if (top != topBottom || bottom != topBottom || left >= maxLeft || left <= minLeft || right >= maxRight || right <= minRight) {
                        errorsList.addError("0", "errorIncorrectMargins");
                    }
                }
            }
        }
    }

    // without checking order of page numbers
    private void checkPageNumbering(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        CTSectPr sectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (sectPr != null) {
            for (XWPFHeader header : xwpfDocument.getHeaderList()) {
                for (XWPFParagraph para : header.getParagraphs()) {
                    boolean isLandscape = isLandscapeOrientation(xwpfDocument);
                    String paraText = para.getText().trim();

                    if (isLandscape) {
                        continue;
                    }

                    if (!paraText.isEmpty() && !paraText.matches("\\d+")) {
                        errorsList.addError("0", "errorPageNumberIncorrectOrMissing");
                    }

                    if (para.getAlignment() != ParagraphAlignment.RIGHT) {
                        errorsList.addError("0","errorPageNumberWrongPlace");
                        break;
                    }
                }
            }
        }
    }

    private boolean isLandscapeOrientation(XWPFDocument xwpfDocument) {
        CTSectPr sectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (sectPr != null && sectPr.getPgSz() != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            int width = ((Number) pgSz.getW()).intValue();
            int height = ((Number) pgSz.getH()).intValue();
            return  width > height;
        }
        return false;
    }
}
