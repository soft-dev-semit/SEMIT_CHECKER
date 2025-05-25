package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.*;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class TitlesCheckTest {
    private String enDoc = "DRB_test_ua_EN.docx";
    private String uaDoc = "DRB_test_ua_UA.docx";

    @Test
    void testCheckTitleNamesEn() throws IOException {
        System.out.println("Checking title headings in english");
        Path path = Paths.get(enDoc);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTitlesCheck().check(document, params, "titles");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTitleNamesUa() throws IOException {
        System.out.println("Checking title headings in ukrainian");
        Path path = Paths.get(uaDoc);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTitlesCheck().check(document, params, "titles");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }
}
