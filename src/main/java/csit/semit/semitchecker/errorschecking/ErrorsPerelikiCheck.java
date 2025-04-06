package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class ErrorsPerelikiCheck implements IErrorsCheckable{

    //TODO Двухглавов - перевірка оформлення переліків

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        return null;
    }
}
