package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ErrorsTablesCheck implements IErrorsCheckable {

    //TODO Ксенія - перевірка оформлення таблиць
    //TODO - додати перевірку послідовності нумерації
    //TODO - додати "Кінець таблиці _._"
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        ErrorsList errors = new ErrorsList(checkParams.getLocaleWord(), checkParams.localeDoc, "table");
        errors.addErrorList(checkTables(xwpfDocument, checkParams, typeErrors));
        System.out.println("CHECKED! ----- " + typeErrors);
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

        if (!document.getTables().isEmpty()) {
            for (int i = 0; i < bodyElements.size(); i++) {
                if (bodyElements.get(i) instanceof XWPFTable table) {
                    XWPFParagraph prevParagraph = (XWPFParagraph) bodyElements.get(i - 1);
                    XWPFParagraph paragraphBFName = (XWPFParagraph) bodyElements.get(i - 2);
                    XWPFParagraph nextParagraph = (XWPFParagraph) bodyElements.get(i + 1);
                    String tableNumber = "Not found";

                    // назва таблиці
                    if (!prevParagraph.getText().matches(maskTableName)) {
                        if (prevParagraph.getText().matches(maskTableCont)) {
                            if (!(bodyElements.get(i - 2) instanceof XWPFTable)) {
                                errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorContNoPrev");
                            }
                        } else {
                            errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoTableName");
                        }
                    } else { // перевіряти стилі тільки якщо були знайдені номери таблиць
                        Pattern pattern = Pattern.compile(maskTableName);
                        Matcher matcher = pattern.matcher(prevParagraph.getText());
                        if (matcher.find()) {
                            tableNumber = matcher.group(1);
                            if (!"TableNumber".equals(prevParagraph.getStyle())) {
                                errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorTableNameStyle");
                            }
                        }
                    }

                    // параграфи до та після таблиці
                    if (!paragraphBFName.getText().isEmpty()) {
                        errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoBlankBfTable");
                    }
                    if (!nextParagraph.getText().isEmpty()) {
                        errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber), "errorNoBlankAfTable");
                    }

                    // перевірка стилів всередині таблиці
                    for (int rowN = 0; rowN < table.getRows().size(); rowN++) {
                        XWPFTableRow row = table.getRow(rowN);
                        for (int cellN = 0; cellN < row.getTableCells().size(); cellN++) {
                            XWPFTableCell cell = row.getCell(cellN);
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                String h1 = bundleWord.getString("H1");
                                String h2 = bundleWord.getString("H2");
                                String h3 = bundleWord.getString("H3");
                                String h4 = bundleWord.getString("H4");

                                if (paragraph.getStyle() != null) {
                                    if (paragraph.getStyle().equals(h1)
                                            || paragraph.getStyle().equals(h2)
                                            || paragraph.getStyle().equals(h3)
                                            || paragraph.getStyle().equals(h4)
                                    ) {
                                        errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber)
                                                        + "[" + rowN + ";" + cellN + "]",
                                                "errorCellStyle");
                                    }
                                } else {
                                    if (paragraph.getRuns().stream().anyMatch(XWPFRun::isBold)
                                            || paragraph.getRuns().stream().anyMatch(XWPFRun::isItalic)
                                            || paragraph.getRuns().stream().anyMatch(run -> run.getUnderline() != UnderlinePatterns.NONE)) {
                                        errors.addError(getTablePlace(table, checkParams, paragraphs, i, tableNumber)
                                                + "[" + rowN + ";" + cellN + "]",
                                                "errorCellStyle");
                                    }
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
            return findHeader(paragraphs, position, params.localeWord) + bundle.getString("tableBeginning") + table.getRow(0).getCell(0).getText().trim() + "\"";
        } else {
            return findHeader(paragraphs, position, params.localeWord) + bundle.getString("tablePosition") + tableNumber;
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
