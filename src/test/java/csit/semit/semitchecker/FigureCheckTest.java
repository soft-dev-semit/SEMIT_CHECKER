package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.*;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class FigureCheckTest {
    String docNameEn = "Figures_test_ua_en.docx";
    String docNameUa = "Figures_test_ua_ua.docx";

    @Test
    void showFigures() throws IOException {
        System.out.println("---------- Checking table cell styles output in ukrainian ----------");
        Path path = Paths.get(docNameUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        for (IBodyElement e : document.getBodyElements()) {
            System.out.print(e.getClass().getSimpleName() + " : " );
            if (e instanceof XWPFParagraph paragraph) {
                if (paragraph.getRuns().stream()
                        .anyMatch(run -> !run.getEmbeddedPictures().isEmpty())
                    || paragraph.getRuns().stream()
                        .anyMatch(run -> !run.getCTR().getDrawingList().isEmpty())) {
                    System.out.print("Picture here");
                }
                System.out.print(paragraph.getText() + "\n");
            }
        }
    }

    @Test
    void testCheckTableNamesEn() throws IOException {
        System.out.println("---------- Checking figure names in english ----------");
        Path path = Paths.get(docNameEn);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsFiguresCheck().check(document, params, "figure");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTableNamesUa() throws IOException {
        System.out.println("---------- Checking figure names in ukrainian ----------");
        Path path = Paths.get(docNameUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsFiguresCheck().check(document, params, "figure");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }
}
