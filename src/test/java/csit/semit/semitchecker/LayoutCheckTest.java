package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class LayoutCheckTest {
    private String enDoc = "titles_layout_text_en.docx";
    private String uaDoc = "titles_layout_text_ua.docx";

    @Test
    void testCheckTitleNamesEn() throws IOException {
        System.out.println("Checking page layout in english");
        Path path = Paths.get(enDoc);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsLayoutCheck().check(document, params, "layout");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTitleNamesUa() throws IOException {
        System.out.println("Checking page layout in ukrainian");
        Path path = Paths.get(uaDoc);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsLayoutCheck().check(document, params, "layout");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }
}
