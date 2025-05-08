package csit.semit.semitchecker.errorschecking;

import lombok.Getter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
public class DocsErrorChecker {

    //Документ
    private XWPFDocument document;
    //Имя файла (бо document -  це байти файлу, фактично - розборка файлу)
    private String docName;

    private CheckParams checkParams;

    private List<IErrorsCheckable> checksToRun;

    private List<ErrorsList> checksResults;

    public List<ErrorsList> getChecksResults() {
        return checksResults;
    }

    public DocsErrorChecker(InputStream inputStream, String docName, CheckParams checkParams) throws IOException {
        document = new XWPFDocument(inputStream);
        this.docName = docName;
        this.checkParams = checkParams;
        checksToRun = createChecksList();
        checksResults = new ArrayList<>();
    }

    private List<IErrorsCheckable> createChecksList() {
        List<IErrorsCheckable> listCh = new ArrayList<>();
        listCh.add(new ErrorsBoundariesCheck());
        listCh.add(new ErrorsPerelikiCheck());
        listCh.add(new ErrorsTablesCheck());
        listCh.add(new ErrorsFiguresCheck());
        return listCh;
    }

    public void checkDoc() {
        for (IErrorsCheckable check : checksToRun) {
            String className = check.getClass().getSimpleName();
            int left = "Errors".length();
            int right = className.lastIndexOf("Check");
            String typeCheck = className.substring(left,right);
            ErrorsList resCheckType = check.check(document,checkParams,typeCheck);
            if ((resCheckType!=null)&&(!resCheckType.getErrors().isEmpty())) {
                checksResults.add(resCheckType);
            }

        }
    }

    public void createTestSet(Locale localeWord, Locale localeDoc, Locale localeInterface) {
        CheckParams testCheckParams = new CheckParams(localeWord,localeDoc, localeInterface);
        if (checksResults!=null) {
            checksResults = new ArrayList<>();
        }
        //list errors 1
        ErrorsList elBoundaries = new ErrorsList(localeWord,localeDoc,"Boundaries");
        elBoundaries.addError("Нема заголовку","BND001");
        elBoundaries.addError("Нема заголовку","BND002");
        checksResults.add(elBoundaries);
        //list errors 2
        ErrorsList elPereliki = new ErrorsList(localeWord,localeDoc,"Pereliki");
        elPereliki.addError("1.2 Маркірований перелік мі... : \"... єдиний пункт переліку.\"","PER001");
        elPereliki.addError("1.4 Маркірований перелік, я... : \"... пункт помилкового переліку 1.\"","PER003");
        elPereliki.addError("1.5 Маркірований перелік із... : \"... пункт помилкового переліку 1;\"","PER002");
        elPereliki.addError("2.2 Нумерований перелік міс... : \"... єдиний пункт переліку.\"","PER001");
        elPereliki.addError("2.4 Нумерований перелік, як... : \"... пункт помилкового переліку 1.\"","PER003");
        elPereliki.addError("2.5 Нумерований перелік із ... : \"... пункт помилкового переліку 1;\"","PER002");
        checksResults.add(elPereliki);
    }

// Old version
//    public void checkDoc() {
//        List<ErrorsList> newRes = new ArrayList<>();
//        for (IErrorsCheckable check : checksToRun) {
//            String className = check.getClass().getSimpleName();
//            int left = "Errors".length();
//            int right = className.lastIndexOf("Check");
//            String typeCheck = className.substring(left,right);
//            newRes.add(check.check(document,checkParams,typeCheck));
//        }
//    }


}
