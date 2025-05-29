package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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
        checkA4Format(xwpfDocument, errorsList, checkParams);
        checkPageMargins(xwpfDocument, errorsList, checkParams);
        //checkPageNumbering(xwpfDocument, errorsList, checkParams);
        return errorsList;
    }

    private List<CTSectPr> getAllSectionProperties(XWPFDocument xwpfDocument) {
        List<CTSectPr> sectPrList = new ArrayList<>();
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();

        CTSectPr firstSectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (firstSectPr != null && !sectPrList.contains(firstSectPr)) {
            sectPrList.add(firstSectPr);
        }

        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            CTSectPr sectPr = paragraph.getCTP().getPPr() != null ? paragraph.getCTP().getPPr().getSectPr() : null;
            if (sectPr != null && !sectPrList.contains(sectPr)) {
                int sectionNumber = checkSectionBreak(xwpfDocument, sectPrList.size(), sectPrList);
                if (sectionNumber > 0 || sectPrList.isEmpty()) {
                    sectPrList.add(sectPr);
                }
            }
        }

        CTSectPr docSectPr = xwpfDocument.getDocument().getBody().getSectPr();
        if (docSectPr != null && !sectPrList.contains(docSectPr)) {
            int sectionNumber = checkSectionBreak(xwpfDocument, sectPrList.size(), sectPrList);
            if (sectionNumber > 0 || sectPrList.isEmpty()) {
                sectPrList.add(docSectPr);
            }
        }
//        System.out.println(sectPrList);

        return sectPrList;
    }

    private void checkA4Format(XWPFDocument xwpfDocument, ErrorsList errorsList, CheckParams checkParams) {
        List<CTSectPr> sectPrList = getAllSectionProperties(xwpfDocument);
        int sectionNumber = 0;

        String section = ResourceBundle.getBundle("resourcesbundles/docskeywords/docskeywords", checkParams.getLocaleInterface()).getString("section");

        for (int i = 0; i < sectPrList.size(); i++) {
            CTSectPr sectPr = sectPrList.get(i);
            sectionNumber = checkSectionBreak(xwpfDocument, i, sectPrList);
            if (sectionNumber == 0) {
                sectionNumber = i + 1;
            }
            if (sectPr != null) {
                CTPageSz pgSz = sectPr.getPgSz();
                if (pgSz != null) {
                    double width = ((Number) pgSz.getW()).doubleValue();
                    double height = ((Number) pgSz.getH()).doubleValue();
                    double expectedWidth = A4_WIDTH_MM * TWENTIETHS_PER_MM;
                    double expectedHeight = A4_HEIGHT_MM * TWENTIETHS_PER_MM;

                    if (isLandscapeOrientation(sectPr)) {
                        if (Math.abs(height - expectedWidth) > EPSILON || Math.abs(width - expectedHeight) > EPSILON) {
                            errorsList.addError(section + " " + sectionNumber, "errorPageFormatIncorrect");
                        }
                    }
                    else {
                        if (Math.abs(width - expectedWidth) > EPSILON || Math.abs(height - expectedHeight) > EPSILON) {
//                            System.out.println("width = " + width + " " + width / TWENTIETHS_PER_MM);
//                            System.out.println("height = " + height + " " + height / TWENTIETHS_PER_MM);
                            errorsList.addError(section + " " + sectionNumber, "errorPageFormatIncorrect");
                        }
                    }
                }
            }
        }
    }

    private void checkPageMargins(XWPFDocument xwpfDocument, ErrorsList errorsList, CheckParams checkParams) {
        List<CTSectPr> sectPrList = getAllSectionProperties(xwpfDocument);
        int sectionNumber = 0;

        String section = ResourceBundle.getBundle("resourcesbundles/docskeywords/docskeywords", checkParams.getLocaleInterface()).getString("section");

        for (int i = 0; i < sectPrList.size(); i++) {
            CTSectPr sectPr = sectPrList.get(i);
            sectionNumber = checkSectionBreak(xwpfDocument, i, sectPrList);
            if (sectionNumber == 0) {
                sectionNumber = i + 1;
            }
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
//                        System.out.println("minLeft = " + minLeft + " " + minLeft / TWENTIETHS_PER_MM);
//                        System.out.println("maxLeft = " + maxLeft + " " + maxLeft / TWENTIETHS_PER_MM);
//                        System.out.println("minRight = " + minRight + " " + minRight / TWENTIETHS_PER_MM);
//                        System.out.println("maxRight = " + maxRight + " " + maxRight / TWENTIETHS_PER_MM);
//                        System.out.println("left = " + left + " " + left / TWENTIETHS_PER_MM);
//                        System.out.println("right = " + right + " " + right / TWENTIETHS_PER_MM);
//                        System.out.println("top = " + top + " " + top / TWENTIETHS_PER_MM);
//                        System.out.println("bottom = " + bottom + " " + bottom / TWENTIETHS_PER_MM);
                        if (isLandscapeOrientation(sectPr)) {
                            if (Math.abs(left - right) > EPSILON || Math.abs(left - topBottom) > EPSILON ||
                                    (top > maxLeft && top - maxLeft > EPSILON) || (top < minLeft && minLeft - top > EPSILON) ||
                                    (bottom > maxRight && bottom - maxRight > EPSILON) || (bottom < minRight && minRight - bottom > EPSILON)) {
                                    errorsList.addError(section + " " + sectionNumber, "errorIncorrectMargins");
                            }
                        }
                        else {
                            if (Math.abs(top - topBottom) > EPSILON || Math.abs(bottom - topBottom) > EPSILON ||
                                    (left > maxLeft && left - maxLeft > EPSILON) || (left < minLeft && minLeft - left > EPSILON) ||
                                    (right > maxRight && right - maxRight > EPSILON) || (right < minRight && minRight - right > EPSILON)) {
                                    errorsList.addError(section + " " + sectionNumber, "errorIncorrectMargins");
                            }
                        }
                    }
                }
            }
        }
    }

    // checking order of page numbers - ?
    private void checkPageNumbering(XWPFDocument xwpfDocument, ErrorsList errorsList, CheckParams checkParams) {
        List<CTSectPr> sectPrList = getAllSectionProperties(xwpfDocument);
        int expectedPageNumber = 2;
        int sectionNumber = 0;

        String section = ResourceBundle.getBundle("resourcesbundles/docskeywords/docskeywords", checkParams.getLocaleDoc()).getString("section");

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
                                errorsList.addError(section + " = " + sectionNumber, "errorPageNumberIncorrectOrMissing");
                            } else {
                                int actualPageNumber = Integer.parseInt(paraText);
                                if (actualPageNumber != expectedPageNumber) {
                                    errorsList.addError(section + sectionNumber + " (Page " + actualPageNumber + ")",
                                            "errorPageNumberOutOfOrder");
                                }
                                expectedPageNumber++;
                            }
                        }

                        if (para.getAlignment() != ParagraphAlignment.RIGHT) {
                            errorsList.addError(section + sectionNumber, "errorPageNumberWrongPlace");
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

    private int checkSectionBreak(XWPFDocument document, int sectionIndex, List<CTSectPr> sectPrList) {
        if (sectionIndex <= 0) {
            return 1;
        }

        CTSectPr currentSectPr = sectPrList.get(sectionIndex);

        if (currentSectPr != null && currentSectPr.isSetType()) {
            CTSectType sectType = currentSectPr.getType();
            if (sectType != null && sectType.isSetVal() && "nextPage".equals(sectType.getVal().toString())) {
                return sectionIndex + 1;
            }
        }

        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);
            CTSectPr paraSectPr = para.getCTP().getPPr() != null ? para.getCTP().getPPr().getSectPr() : null;
            if (paraSectPr != null && paraSectPr.equals(currentSectPr)) {
                for (XWPFRun run : para.getRuns()) {
                    String text = run.getText(0);
                    if (text != null && text.contains("\f")) {
                        return sectionIndex + 1;
                    }

                    if (run.getCTR() != null) {
                        CTBr[] breakElements = run.getCTR().getBrArray();
                        if (breakElements != null && breakElements.length > 0) {
                            for (CTBr br : breakElements) {
                                if (br.isSetType() && "page".equals(br.getType().toString())) {
                                    return sectionIndex + 1;
                                }
                            }
                        }

                        XmlObject xmlObj = run.getCTR();
                        Node node = xmlObj.getDomNode();
                        NodeList childNodes = node.getChildNodes();
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node childNode = childNodes.item(j);
                            if ("lastRenderedPageBreak".equals(childNode.getLocalName())) {
                                return sectionIndex + 1;
                            }
                        }
                    }
                }

                if (para.getCTP() != null && para.getCTP().getPPr() != null && para.getCTP().getPPr().isSetPageBreakBefore()) {
                    return sectionIndex + 1;
                }
            }
        }

        if (sectionIndex > 0) {
            CTSectPr prevSectPr = sectPrList.get(sectionIndex - 1);
            if (prevSectPr != null && prevSectPr.isSetType()) {
                CTSectType prevSectType = prevSectPr.getType();
                if (prevSectType != null && prevSectType.isSetVal() && "nextPage".equals(prevSectType.getVal().toString())) {
                    return sectionIndex + 1;
                }
            }
        }

        for (int i = sectionIndex - 1; i >= 0; i--) {
            CTSectPr prevSectPr = sectPrList.get(i);
            for (int j = 0; j < paragraphs.size(); j++) {
                XWPFParagraph p = paragraphs.get(j);
                CTSectPr paraSectPr = p.getCTP().getPPr() != null ? p.getCTP().getPPr().getSectPr() : null;
                if (paraSectPr != null && paraSectPr.equals(prevSectPr)) {
                    if (!p.getText().trim().isEmpty() && i < sectionIndex - 1) {
                        break;
                    }

                    for (XWPFRun r : p.getRuns()) {
                        if (r.getCTR() != null) {
                            CTBr[] breakElements = r.getCTR().getBrArray();
                            if (breakElements != null && breakElements.length > 0) {
                                return sectionIndex + 1;
                            }

                            XmlObject xmlObj = r.getCTR();
                            Node node = xmlObj.getDomNode();
                            NodeList childNodes = node.getChildNodes();
                            for (int k = 0; k < childNodes.getLength(); k++) {
                                Node childNode = childNodes.item(k);
                                if ("lastRenderedPageBreak".equals(childNode.getLocalName())) {
                                    return sectionIndex + 1;
                                }
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }
}
