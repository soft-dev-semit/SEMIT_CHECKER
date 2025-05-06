package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.*;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ErrorsTablesCheck implements IErrorsCheckable{

    //TODO Ксенія - перевірка оформлення таблиць
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        return null;
    }

    public ErrorsList checkTableNames(XWPFDocument document, CheckParams checkParams, String typeErrors) {
        ResourceBundle bundle = ResourceBundle.getBundle("resoursesboundles/table", checkParams.getLocaleInterfaces());
        ErrorsList errors = new ErrorsList(checkParams.localeDoc, checkParams.localeWord, typeErrors);
        List<IBodyElement> bodyElements = document.getBodyElements();

        String maskTableName = bundle.getString("maskTableName");
        String maskTableCont = bundle.getString("maskTableCont");

        if (!document.getTables().isEmpty()) {
            for (int i = 0; i < bodyElements.size(); i++) {
                if (bodyElements.get(i) instanceof XWPFTable table) {
                    XWPFParagraph prevParagraph = (XWPFParagraph) bodyElements.get(i - 1);
                    XWPFParagraph paragraphBFName = (XWPFParagraph) bodyElements.get(i - 2);
                    XWPFParagraph nextParagraph = (XWPFParagraph) bodyElements.get(i + 1);

                    if (!paragraphBFName.getText().isEmpty()) {
                        errors.addError(getTablePlace(table, checkParams), bundle.getString("errorNoBlankBf"));
                    }
                    if (!nextParagraph.getText().isEmpty()) {
                        errors.addError(getTablePlace(table, checkParams), bundle.getString("errorNoBlankAf"));
                    }
                    if (!prevParagraph.getText().matches(maskTableName)) {
                        if (prevParagraph.getText().matches(maskTableCont)) {
                            if (!(bodyElements.get(i - 3) instanceof XWPFTable)) {
                                errors.addError(getTablePlace(table, checkParams), bundle.getString("errorContNoPrev"));
                            }
                        } else {
                            errors.addError(getTablePlace(table, checkParams), bundle.getString("errorNoName"));
                        }
                    }
                }
            }
        }
        return errors;
    }


    private String getTablePlace(XWPFTable table, CheckParams params) {
        ResourceBundle bundle = ResourceBundle.getBundle("resoursesboundles/table", params.getLocaleInterfaces());
        return bundle.getString("tableBeginning") + table.getRow(0).getCell(0).getText().trim() + "\"";
    }

    // for checking table numbering??
    private List<XWPFParagraph> findAllTableNames(XWPFDocument xwpfDocument, CheckParams checkParams) {
        String maskTableName = ResourceBundle.getBundle("resoursesboundles/table", checkParams.getLocaleDoc()).getString("maskTableName");
        return xwpfDocument.getParagraphs().stream()
                    .filter(p -> p.getText().matches(maskTableName))
                    .collect(Collectors.toList());
    }

    private List<XWPFParagraph> findAllTableConts(XWPFDocument xwpfDocument, CheckParams checkParams) {
        String maskTableCont = ResourceBundle.getBundle("resoursesboundles/table", checkParams.getLocaleDoc()).getString("maskTableCont");
        return xwpfDocument.getParagraphs().stream()
                    .filter(p -> p.getText().matches(maskTableCont))
                    .collect(Collectors.toList());
    }




}
