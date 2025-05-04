package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class ErrorsTablesCheck implements IErrorsCheckable{

    //TODO Ксенія - перевірка оформлення таблиць
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        System.out.println("CHECKED! ----- "+typeErrors);
        return null;
    }
}
