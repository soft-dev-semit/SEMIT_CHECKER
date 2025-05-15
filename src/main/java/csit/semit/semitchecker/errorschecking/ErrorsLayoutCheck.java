package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.util.ArrayList;
import java.util.List;

public class ErrorsLayoutCheck implements IErrorsCheckable {
    private static final double TWENTIETHS_PER_MM = 56.692; // 1 мм через одиницю виміру у Word
    private static final int A4_WIDTH_MM = 210;
    private static final int A4_HEIGHT_MM = 297;
    private static final int TOP_BOTTOM_MARGIN_MM = 20; // зверху, знизу
    private static final int MIN_LEFT_MARGIN_MM = 30;  // ліворуч
    private static final int MAX_LEFT_MARGIN_MM = 35;
    private static final int MIN_RIGHT_MARGIN_MM = 10; // праворуч
    private static final int MAX_RIGHT_MARGIN_MM = 15;
    private static final double EPSILON = 1.0;

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errorsList = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        checkA4Format(xwpfDocument, errorsList);
        checkPageMargins(xwpfDocument, errorsList);
        //checkPageNumbering(xwpfDocument, errorsList);
        return errorsList;
    }

    private List<CTSectPr> getAllSectionProperties(XWPFDocument xwpfDocument) {
        List<CTSectPr> sectPrList = new ArrayList<>();

        for (XWPFParagraph paragraph : xwpfDocument.getParagraphs()) {
            CTSectPr sectPr = paragraph.getCTPPr().getSectPr();
            if (sectPr != null) {
                sectPrList.add(sectPr);
            }
        }

        // додавання властивостей секції документу (остання секція)
        CTSectPr docSectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (docSectPr != null && !sectPrList.contains(docSectPr)) {
            sectPrList.add(docSectPr);
        }

        return sectPrList;
    }

    private void checkA4Format(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        List<CTSectPr> sectPrList = getAllSectionProperties(xwpfDocument);
        int sectionNumber = 0;

        for (CTSectPr sectPr : sectPrList) {
            sectionNumber++;
            if (sectPr != null) {
                CTPageSz pgSz = sectPr.getPgSz();
                if (pgSz != null) {
                    double width = ((Number) pgSz.getW()).doubleValue();
                    double height = ((Number) pgSz.getH()).doubleValue();
                    double expectedWidth = A4_WIDTH_MM * TWENTIETHS_PER_MM;
                    double expectedHeight = A4_HEIGHT_MM * TWENTIETHS_PER_MM;

                    if (isLandscapeOrientation(sectPr)) {
                        if (Math.abs(height - expectedWidth) > EPSILON || Math.abs(width - expectedHeight) > EPSILON) {
                            errorsList.addError("Section " + sectionNumber, "errorPageFormatIncorrect");
                        }
                    }
                    else {
                        if (Math.abs(width - expectedWidth) > EPSILON || Math.abs(height - expectedHeight) > EPSILON) {
                            errorsList.addError("Section " + sectionNumber, "errorPageFormatIncorrect");
                        }
                    }
                }
            }
        }
    }

    private void checkPageMargins(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        List<CTSectPr> sectPrList = getAllSectionProperties(xwpfDocument);
        int sectionNumber = 0;

        for (CTSectPr sectPr : sectPrList) {
            sectionNumber++;
            if (sectPr != null) {
                CTPageSz pgSz = sectPr.getPgSz();
                if (pgSz != null) {
                    CTPageMar pgMar = sectPr.getPgMar();
                    if (pgMar != null) {
                        double left = ((Number) pgMar.getLeft()).doubleValue();
                        double right = ((Number) pgMar.getRight()).doubleValue();
                        double top = ((Number) pgMar.getTop()).doubleValue();
                        double bottom = ((Number) pgMar.getBottom()).doubleValue();
                        double topBottom = TOP_BOTTOM_MARGIN_MM * TWENTIETHS_PER_MM;
                        double minLeft = MIN_LEFT_MARGIN_MM * TWENTIETHS_PER_MM;
                        double maxLeft = MAX_LEFT_MARGIN_MM * TWENTIETHS_PER_MM;
                        double minRight = MIN_RIGHT_MARGIN_MM * TWENTIETHS_PER_MM;
                        double maxRight = MAX_RIGHT_MARGIN_MM * TWENTIETHS_PER_MM;

                        if (isLandscapeOrientation(sectPr)) {
                            if (Math.abs(left - right) > EPSILON || Math.abs(left - topBottom) > EPSILON
                                    || top > maxLeft || top < minLeft || bottom > maxRight || bottom < minRight) {
                                errorsList.addError("Section " + sectionNumber, "errorIncorrectMargins");
                            }
                        }
                        else {
                            if (Math.abs(top - topBottom) > EPSILON || Math.abs(bottom - topBottom) > EPSILON
                                    || left > maxLeft || left < minLeft || right > maxRight || right < minRight) {
                                errorsList.addError("Section " + sectionNumber, "errorIncorrectMargins");
                            }
                        }
                    }
                }
            }
        }
    }

    // checking order of page numbers - ?
    private void checkPageNumbering(XWPFDocument xwpfDocument, ErrorsList errorsList) {
        List<CTSectPr> sectPrList = getAllSectionProperties(xwpfDocument);
        int expectedPageNumber = 2;
        int sectionNumber = 0;

        for (CTSectPr sectPr : sectPrList) {
            sectionNumber++;
            if (sectPr != null) {
                int sectionStart = expectedPageNumber;
                if (sectPr.getPgNumType() != null && sectPr.getPgNumType().getStart() != null) {
                    sectionStart = sectPr.getPgNumType().getStart().intValue();
                    expectedPageNumber = sectionStart; // Adjust based on section start
                }

                for (XWPFHeader header : xwpfDocument.getHeaderList()) {
                    for (XWPFParagraph para : header.getParagraphs()) {
                        boolean isLandscape = isLandscapeOrientation(sectPr);
                        String paraText = para.getText().trim();

                        if (isLandscape) {
                            continue;
                        }

                        if (!paraText.isEmpty()) {
                            if (!paraText.matches("[1-9]\\d*")) {
                                errorsList.addError("Section " + sectionNumber, "errorPageNumberIncorrectOrMissing");
                            } else {
                                int actualPageNumber = Integer.parseInt(paraText);
                                if (actualPageNumber != expectedPageNumber) {
                                    errorsList.addError("Section " + sectionNumber + " (Page " + actualPageNumber + ")",
                                            "errorPageNumberOutOfOrder");
                                }
                                expectedPageNumber++;
                            }
                        }

                        if (para.getAlignment() != ParagraphAlignment.RIGHT) {
                            errorsList.addError("Section " + sectionNumber, "errorPageNumberWrongPlace");
                        }
                    }
                }
            }
        }
    }

    private boolean isLandscapeOrientation(CTSectPr sectPr) {
        if (sectPr != null && sectPr.getPgSz() != null) {
            CTPageSz pgSz = sectPr.getPgSz();
            int width = ((Number) pgSz.getW()).intValue();
            int height = ((Number) pgSz.getH()).intValue();
            return  width > height;
        }
        return false;
    }
}
