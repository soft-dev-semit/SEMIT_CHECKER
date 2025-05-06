package csit.semit.semitchecker.docutils;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.util.ArrayList;
import java.util.List;

public class CheckPageLayout {
    private static final double TWENTIETHS_PER_MM = 56.692; // 1 мм через одиницю виміру у Word
    private static final int A4_WIDTH_MM = 210;
    private static final int A4_HEIGHT_MM = 297;
    private static final int MIN_MARGIN_MM = 20; // ліворуч, зверху, знизу
    private static final int MIN_RIGHT_MARGIN_MM = 10; // праворуч

    private XWPFDocument document;
    private List<String> errorsList;
    private CTSectPr sectPr;

    public CheckPageLayout(XWPFDocument document, CTSectPr sectPr, List<String> errorsList) {
        this.document = document;
        this.sectPr = this.document.getDocument().getBody().getSectPr();
        this.errorsList = new ArrayList<String>();
    }

    private void checkA4Fromat() {
        if (sectPr != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            if (pgSz != null) {
                int width = ((Number) pgSz.getW()).intValue();
                int height = ((Number) pgSz.getH()).intValue();
                if (width != (int) (A4_WIDTH_MM * TWENTIETHS_PER_MM) || height != (int) (A4_HEIGHT_MM * TWENTIETHS_PER_MM)) {
                    errorsList.add("wrong-page-size");
                }
            }
        }
    }

    private void checkPageMargins() {
        if (sectPr != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            if (pgSz != null) {
                CTPageMar pgMar = sectPr.getPgMar();
                if (pgMar != null) {
                    int left = ((Number) pgMar.getLeft()).intValue();
                    int right = ((Number) pgMar.getRight()).intValue();
                    int top = ((Number) pgMar.getTop()).intValue();
                    int bottom = ((Number) pgMar.getBottom()).intValue();
                    int minLeftTopBottom = (int) (MIN_MARGIN_MM * TWENTIETHS_PER_MM);
                    int minRight = (int) (MIN_RIGHT_MARGIN_MM * TWENTIETHS_PER_MM);
                    //потрібно, щоб границі були не менше або точно дорівнювали? яке максимальне значення відступів від границь?
                    if (left < minLeftTopBottom || top < minLeftTopBottom || bottom < minLeftTopBottom || right < minRight) {
                        errorsList.add(String.format("Margins too small: left=%d, right=%d, top=%d, bottom=%d (in twentieths)",
                                left, right, top, bottom));
                    }
                }
            }
        }
    }

    // потрібно перевірити порядок номерів сторінок через Libre Office
    private void checkPageNumbering() {
        if (sectPr != null) {
            for (XWPFHeader header : document.getHeaderList()) {
                for (XWPFParagraph para : header.getParagraphs()) {
                    boolean isLandscape = isLandscapeOrientation();
                    String paraText = para.getText().trim();

                    if (isLandscape) {
                        continue;
                    }

                    if (!paraText.isEmpty() && !paraText.matches("\\d+")) { // Basic check for number presence
                        errorsList.add("Page number missing or invalid in non-landscape section");
                    }

                    if (para.getAlignment() != ParagraphAlignment.RIGHT) {
                        errorsList.add("Page number not in top-right corner");
                        break;
                    }
                }
            }
        }
    }

    private boolean isLandscapeOrientation() {
        if (sectPr != null && sectPr.getPgSz() != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            int width = ((Number) pgSz.getW()).intValue();
            int height = ((Number) pgSz.getH()).intValue();
            return  width > height;
        }
        return false;
    }

    public void performAllChecks() {
        checkA4Fromat();
        checkPageMargins();
        checkPageNumbering();
    }
}
