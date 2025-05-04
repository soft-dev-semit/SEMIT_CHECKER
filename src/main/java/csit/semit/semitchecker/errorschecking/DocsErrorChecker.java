package csit.semit.semitchecker.errorschecking;

import lombok.Getter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DocsErrorChecker {

    //Документ
    private XWPFDocument document;
    //Имя файла (бо document -  це байти файлу, фактично - розборка файлу)
    private String docName;

    private CheckParams checkParams;

    private List<IErrorsCheckable> checksToRun;

    private List<ErrorsList> checksResults;

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
        return listCh;
    }

    public void checkDoc() {
        List<ErrorsList> newRes = new ArrayList<>();
        for (IErrorsCheckable check : checksToRun) {
            String className = check.getClass().getSimpleName();
            int left = "Errors".length();
            int right = className.lastIndexOf("Check");
            String typeCheck = className.substring(left,right);
            newRes.add(check.check(document,checkParams,typeCheck));
        }
    }

    public void showCheckResults() {


    }

}
