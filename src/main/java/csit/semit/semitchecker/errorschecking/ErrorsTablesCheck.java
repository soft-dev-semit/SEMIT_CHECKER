package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ErrorsTablesCheck implements IErrorsCheckable {

    //TODO Ксенія - перевірка оформлення таблиць
    //TODO К - додати перевірку послідовності нумерації
    //TODO К - додати перевірку наявності посилань
    // Подумати про вивід повідомлень для продовження та кінця таблиці

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errors = new ErrorsList(checkParams.getLocaleWord(), checkParams.localeDoc, "table");
        errors.addErrorList(checkTables(xwpfDocument, checkParams, typeErrors));
        return errors;
    }

    // перевірка назви, пустих рядків навколо, стилів назви, стилів всередині таблиці
    public ErrorsList checkTables(XWPFDocument document, CheckParams checkParams, String typeErrors) {
        ResourceBundle bundleDoc = ResourceBundle.getBundle("resourcesbundles/errorstexts/table", checkParams.getLocaleDoc());
        ResourceBundle bundleWord = ResourceBundle.getBundle("resourcesbundles.docstyles.docswordstyles", checkParams.getLocaleWord());
        ErrorsList errors = new ErrorsList(checkParams.localeDoc, checkParams.localeWord, typeErrors);
        List<IBodyElement> bodyElements = document.getBodyElements();
        List<XWPFParagraph> paragraphs = bodyElements.stream()
                .map(e -> e instanceof XWPFParagraph ? (XWPFParagraph) e : null)
                .collect(Collectors.toList());


        String maskTableName = bundleDoc.getString("maskTableName");
        String maskTableCont = bundleDoc.getString("maskTableCont");
        String maskTableEnd = bundleDoc.getString("maskTableEnd");

        if (!document.getTables().isEmpty()) {
            for (int i = 0; i < bodyElements.size(); i++) {
                if (bodyElements.get(i) instanceof XWPFTable table) {
                    // ворд автоматично об'єднує таблиці які стоять одна за однією, винятку не повинно бути
                    XWPFParagraph prevParagraph = (XWPFParagraph) bodyElements.get(i - 1);
                    XWPFParagraph nextParagraph = (XWPFParagraph) bodyElements.get(i + 1);
                    String tableNumber = "Not found";

                    if (prevParagraph.getText().matches(maskTableName)) { // table 1.1 - table name
                        tableNumber = findTableNumber(prevParagraph, maskTableName);
                        if (!"Not found".equals(tableNumber)) { // checking style for table name
                            if (!"TableNumber".equals(prevParagraph.getStyle())) {
                                errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorTableNameStyle");
                            }
                        }
                        if (!(nextParagraph.getText().isEmpty())) {
                            if (!(nextParagraph.getText().matches(maskTableEnd) || nextParagraph.getText().matches(maskTableCont))) {
                                errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoBlankAfTable");
                            }
                        }
                        if (bodyElements.get(i - 2) instanceof XWPFParagraph p && !p.getText().isEmpty()) {
                            errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoBlankBfTable");
                        }
                    } else if (prevParagraph.getText().matches(maskTableCont)) { // continuation of a table 1.1
                        tableNumber = findTableNumber(prevParagraph, maskTableCont);
                        if (!(bodyElements.get(i - 2) instanceof XWPFTable)) { // has to have table before it
                            errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorContNoPrev");
                        }
                        if (!(bodyElements.get(i + 2) instanceof XWPFTable)) { // has to have table after it
                            errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorContNoEnd");
                        }
                        if (!"Not found".equals(tableNumber)) { // checking style for table cont
                            if (!"TableNumber".equals(prevParagraph.getStyle())) {
                                errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorTableContStyle");
                            }
                        }
                    } else if (prevParagraph.getText().matches(maskTableEnd)) { // end of table 1.1
                        tableNumber = findTableNumber(prevParagraph, maskTableEnd);
                        if (!(bodyElements.get(i - 2) instanceof XWPFTable)) { // has to have table before it
                            errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorEndNoPrev");
                        }
                        if (!nextParagraph.getText().isEmpty()) {
                            errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoBlankAfTable");
                        }
                    } else { // name wasn't found
                        errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoTableName");
                    }

                    // check styles inside the table
                    Set<String> cellForbiddenStyles = Set.of(
                            bundleWord.getString("H1"),
                            bundleWord.getString("H2"),
                            bundleWord.getString("H3"),
                            bundleWord.getString("H4"),
                            "TableNumber",
                            "FigureNumber"
                    );
                    List<XWPFTableRow> rows = table.getRows();
                    for (int rowN = 0; rowN < rows.size(); rowN++) {
                        XWPFTableRow row = rows.get(rowN);
                        List<XWPFTableCell> cells = row.getTableCells();
                        for (int cellN = 0; cellN < cells.size(); cellN++) {
                            XWPFTableCell cell = cells.get(cellN);
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                String style = paragraph.getStyle();

                                boolean hasBadStyle = (style != null && cellForbiddenStyles.contains(style)) ||
                                        paragraph.getRuns().stream().anyMatch(run ->
                                                !(run.getColor() == null || "000000".equals(run.getColor()))
                                                || run.isBold() || run.isItalic() || run.getUnderline() != UnderlinePatterns.NONE);

                                if (hasBadStyle) {
                                    errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber)
                                                    + ", [" + (rowN + 1) + ";" + (cellN + 1) + "]",
                                            "errorCellStyle");
                                }
                            }
                        }
                    }

                }
            }
        }
        return errors;
    }

    private String getTablePlace(XWPFTable table, CheckParams params, @NotNull List<XWPFParagraph> paragraphs, int position, String tableNumber) {
        ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles/errorstexts/table", params.getLocaleInterface());
        if (tableNumber.equals("Not found")) {
            return findHeader(paragraphs, position, params) + bundle.getString("tableBeginning") + table.getRow(0).getCell(0).getText().trim() + "\"";
        } else {
            return bundle.getString("tablePosition") + tableNumber;
        }
    }

    private String findTableNumber(XWPFParagraph p, String mask) {
        Pattern pattern = Pattern.compile(mask);
        Matcher matcher = pattern.matcher(p.getText());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Not found";
        }
    }

    // for table numbering checks??
    private List<XWPFParagraph> findAllTableNames(XWPFDocument xwpfDocument, CheckParams checkParams) {
        String maskTableName = ResourceBundle.getBundle("resourcesbundles/errorstexts/table", checkParams.getLocaleDoc()).getString("maskTableName");
        return xwpfDocument.getParagraphs().stream()
                    .filter(p -> p.getText().matches(maskTableName))
                    .collect(Collectors.toList());
    }

    private List<XWPFParagraph> findAllTableConts(XWPFDocument xwpfDocument, CheckParams checkParams) {
        String maskTableCont = ResourceBundle.getBundle("resourcesbundles/errorstexts/table", checkParams.getLocaleDoc()).getString("maskTableCont");
        return xwpfDocument.getParagraphs().stream()
                    .filter(p -> p.getText().matches(maskTableCont))
                    .collect(Collectors.toList());
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
            XWPFParagraph p = paragraphs.get(i);
            String style = p != null ? p.getStyle() : null;
            if (style != null && headers.contains(style)) {
                int endIdx = p.getText().length();
                String app = ResourceBundle
                        .getBundle("resourcesbundles.docskeywords.docskeywords", checkParams.getLocaleDoc())
                        .getString("dodatok");
                if (p.getText().toLowerCase().contains(app.toLowerCase())){
                    endIdx = app.length() + 2;
                } else {
                    endIdx = Character.getNumericValue(style.charAt(style.length() - 1)) + 1;
                }
                return p.getText().substring(0, endIdx) + "... ";
            }
        }
        return noHeader + " ";
    }
}
