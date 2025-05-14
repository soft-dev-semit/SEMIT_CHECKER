package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



//TODO К - додати перевірку послідовності нумерації
//TODO К - додати перевірку наявності посилань
public class ErrorsFiguresCheck implements IErrorsCheckable {
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errors = new ErrorsList(checkParams.getLocaleWord(), checkParams.localeDoc, "figure");
        errors.addErrorList(checkFigures(xwpfDocument, checkParams, typeErrors));
        return errors;
    }

    // перевірка назви, пустих рядків навколо, стилів назви
    public ErrorsList checkFigures(XWPFDocument document, CheckParams checkParams, String typeErrors) {
        ResourceBundle bundleDoc = ResourceBundle.getBundle("resourcesbundles/errorstexts/figure", checkParams.getLocaleDoc());
        ErrorsList errors = new ErrorsList(checkParams.localeDoc, checkParams.localeWord, typeErrors);
        List<IBodyElement> bodyElements = document.getBodyElements();
        List<CTDrawing> pictures = document.getBodyElements().stream()
                .filter(e -> e instanceof XWPFParagraph)
                .map(e -> (XWPFParagraph) e)
                .flatMap(p -> p.getRuns().stream()
                        .flatMap(r -> r.getCTR().getDrawingList().stream()))
                .toList();

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
                        XWPFParagraph nextParagraph = (XWPFParagraph) bodyElements.get(i + 1); //name should be here
                        String figureNumber = "not found";

                        if (!nextParagraph.getText()
                                .replace("\r", "")
                                .replace("\n", "")
                                .matches(maskFigureName)) {
                            errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNoFigureName");
                        } else { // перевіряти стилі тільки якщо були знайдені номери рисунків
                            Pattern pattern = Pattern.compile(maskFigureName);
                            Matcher matcher = pattern.matcher(nextParagraph.getText().replace("\r", ""));
                            if (matcher.find()) {
                                figureNumber = matcher.group(1);
                                if (!"FigureNumber".equals(nextParagraph.getStyle())) {
                                    errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorFigureNameStyle");
                                }
                            }
                        }

                        // параграф до рисунка
                        if (!prevParagraph.getText().isEmpty()) {
                            errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNoBlankBfFigure");
                        }
                        // параграф після рисунку
                        if (i < bodyElements.size() - 2) { // check that the picture is not last element of the doc
                            XWPFParagraph paragraphAFName = (XWPFParagraph) bodyElements.get(i + 2); //should be empty
                            if (!paragraphAFName.getText().isEmpty()) {
                                errors.addError(getFigurePlace(checkParams, paragraphs, i, figureNumber), "errorNoBlankAfFigure");
                            }
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
            String pos = "";
            for (int i = position; i >= 0; i--) {
                if (paragraphs.get(i) != null && !paragraphs.get(i).getText().isEmpty()) {
                    pos = paragraphs.get(i).getText().substring(0, Math.min(paragraphs.get(i).getText().length(), 100));
                    break;
                }
            }

            return findHeader(paragraphs, position, params) + bundle.getString("figureBeginning") + pos +  "\"";
        } else {
            return bundle.getString("figurePosition") + figureNumber;
        }
    }

    public String findHeader(@NotNull List<XWPFParagraph> paragraphs, int startPos, CheckParams checkParams) {
        ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles.docstyles.docswordstyles", checkParams.getLocaleWord());
        String noHeader = bundle.getString("noheader");
        Set<String> headers = Set.of(
                bundle.getString("H1"),
                bundle.getString("H2"),
                bundle.getString("H3"),
                bundle.getString("H4")
        );

        for (int i = startPos; i >= 0; i--) {
            if (paragraphs.get(i) != null) {
                XWPFParagraph p = paragraphs.get(i);
                String style = p != null ? p.getStyle() : null;
                if (style != null && headers.contains(style)) {
                    int endIdx = Math.min(p.getText().length(), 27);
                    String app = ResourceBundle
                            .getBundle("resourcesbundles.docskeywords.docskeywords", checkParams.getLocaleDoc())
                            .getString("dodatok");
                    if (p.getText().toLowerCase().contains(app.toLowerCase())) {
                        endIdx = app.length() + 2;
                    } else if (p.getText().substring(0, 1).matches("\\d")) {
                        endIdx = Character.getNumericValue(style.charAt(style.length() - 1)) + 1;
                    }
                    return p.getText().substring(0, endIdx) + "... ";
                }
            }
        }
        return noHeader + " ";
    }


}
