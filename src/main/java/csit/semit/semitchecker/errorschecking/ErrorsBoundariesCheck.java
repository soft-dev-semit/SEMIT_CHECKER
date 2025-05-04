package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class ErrorsBoundariesCheck implements IErrorsCheckable{

    //TODO Анастасія - перевірка відступів сторінок: ліво 3-3.5,  право - 1-1.5, верх та низ - 2
    //Деякі сторінки можуть бути в горизонтальній орієнтації
    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {

        System.out.println("CHECKED! ----- "+typeErrors);
        return null;
    }
}
