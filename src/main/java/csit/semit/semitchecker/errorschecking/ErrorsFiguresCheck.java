package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ErrorsFiguresCheck implements IErrorsCheckable {
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errors = new ErrorsList(checkParams.getLocaleWord(), checkParams.localeDoc, "figure");
        errors.addErrorList(checkFigures(xwpfDocument, checkParams, typeErrors));
        System.out.println("CHECKED! ----- " + typeErrors);
        return errors;
    }

    // перевірка назви, пустих рядків навколо, стилів назви
    public ErrorsList checkFigures(XWPFDocument document, CheckParams checkParams, String typeErrors) {
        ResourceBundle bundleDoc = ResourceBundle.getBundle("resourcesbundles/errorstexts/figure", checkParams.getLocaleDoc());
        ResourceBundle bundleWord = ResourceBundle.getBundle("resourcesbundles.docstyles.docswordstyles", checkParams.getLocaleWord());
        ErrorsList errors = new ErrorsList(checkParams.localeDoc, checkParams.localeWord, typeErrors);
        List<IBodyElement> bodyElements = document.getBodyElements();
        List<XWPFPictureData> pictures = document.getAllPictures();
        List<XWPFParagraph> paragraphs = bodyElements.stream()
                .map(e -> e instanceof XWPFParagraph ? (XWPFParagraph) e : null)
                .collect(Collectors.toList());

        String maskFigureName = bundleDoc.getString("maskFigureName");

        if (!pictures.isEmpty()) {
            for (int i = 0; i < bodyElements.size(); i++) {
                if (bodyElements.get(i) instanceof XWPFParagraph paragraph) {
                    if (paragraph.getRuns().stream()
                            .anyMatch(run -> !run.getEmbeddedPictures().isEmpty())
                            || paragraph.getRuns().stream()
                            .anyMatch(run -> !run.getCTR().getDrawingList().isEmpty())
                    ) {
                        // абзац і має рисунок
                        XWPFParagraph prevParagraph = (XWPFParagraph) bodyElements.get(i - 1); //should be empty
                        XWPFParagraph paragraphAFName = (XWPFParagraph) bodyElements.get(i + 2); //should be empty
                        XWPFParagraph nextParagraph = (XWPFParagraph) bodyElements.get(i + 1); //name should be here
                        String figureNumber = "not found";

                        if (!nextParagraph.getText().matches(maskFigureName)) {
                            errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNoName");
                        } else { // перевіряти стилі тільки якщо були знайдені номери рисунків
                            Pattern pattern = Pattern.compile(maskFigureName);
                            Matcher matcher = pattern.matcher(nextParagraph.getText());
                            if (matcher.find()) {
                                figureNumber = matcher.group(1);
                                if (!"FigureNumber".equals(nextParagraph.getStyle())) {
                                    errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNameStyle");
                                }
                            }
                        }

                        // параграфи до та після рисунку
                        if (!paragraphAFName.getText().isEmpty()) {
                            errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNoBlankAf");
                        }
                        if (!prevParagraph.getText().isEmpty()) {
                            errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNoBlankBf");
                        }

                        // стиль абзацу з рисунком
                        if (!"Figure".equals(paragraph.getStyle())) {
                            errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorFigureStyle");
                        }


                    }
                }
            }
        }


        return errors;
    }

    private String getFigurePlace(CheckParams params, @NotNull List<XWPFParagraph> paragraphs, int position, String figureNumber) {
        ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles/errorstexts/figure", params.getLocaleInterface());
        if (figureNumber.equals("not found")) {
            return findHeader(paragraphs, position, params.localeWord) + bundle.getString("figureBeginning")
                    + paragraphs.get(position - 2).getText().substring(0, Math.min(paragraphs.get(position - 2).getText().length(), 15)) + "\"";
        } else {
            return findHeader(paragraphs, position, params.localeWord) + bundle.getString("figurePosition") + figureNumber;
        }
    }

    public String findHeader(@NotNull List<XWPFParagraph> xwpfParagraphs, int posStartFind, Locale localWord) {
        //Готуються дані про стилі в залежності від призначеної локації
        //Загрузити локацію та назви стилів заголовків
        ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles.docstyles.docswordstyles", localWord);
        String noheader = bundle.getString("noheader");
        String h1 = bundle.getString("H1");
        String h2 = bundle.getString("H2");
        String h3 = bundle.getString("H3");
        String h4 = bundle.getString("H4");
        //Визначається заголовок частини документу, в якому знайдений перелік
        String place = noheader;
        int i = posStartFind;
        boolean findEnd = false;
        XWPFParagraph p;
        do {
            p = xwpfParagraphs.get(i);
            if (p != null) {
                if (p.getStyle() != null) {
                    if (p.getStyle().equals(h4) || p.getStyle().equals(h3) || p.getStyle().equals(h2) || p.getStyle().equals(h1)) {
                        int sizeHeader = Math.min(p.getText().length(), 27);
                        place = p.getText().substring(0, sizeHeader) + "... ";
                        findEnd = true;
                    }
                }
            }
            if (!findEnd) {
                i--;
            }
        } while (i >= 0 && !findEnd);
        return place;
    }

}
