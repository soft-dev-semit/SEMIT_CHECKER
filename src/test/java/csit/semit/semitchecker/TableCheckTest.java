package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.CheckError;
import csit.semit.semitchecker.errorschecking.CheckParams;
import csit.semit.semitchecker.errorschecking.ErrorsList;
import csit.semit.semitchecker.errorschecking.ErrorsTablesCheck;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class TableCheckTest {
    String docNameEn = "Tables_test_ua_en.docx";
    String docNameUa = "Tables_test_ua_ua.docx";

    @Test
    void testCheckTableNamesEn() throws IOException {
        System.out.println("---------- Checking table names in english ----------");
        Path path = Paths.get(docNameEn);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTablesCheck().checkTableNames(document, params, "table");
        for (CheckError error : errors.getErrorList()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTableNamesUa() throws IOException {
        System.out.println("---------- Checking table names in ukrainian ----------");
        Path path = Paths.get(docNameUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTablesCheck().checkTableNames(document, params, "table");
        for (CheckError error : errors.getErrorList()) {
            System.out.println(error);
        }
    }

}
