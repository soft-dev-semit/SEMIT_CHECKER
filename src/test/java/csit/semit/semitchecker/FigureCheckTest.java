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
    String docNameUaEn = "tables_figures_test_ua_en.docx";
    String docNameUaUa = "tables_figures_test_ua_ua.docx";
    String docNameEnEn = "tables_figures_test_en_en.docx";
    String docNameEnUa = "tables_figures_test_en_ua.docx";

    void showFigures() throws IOException {
        System.out.println("---------- Checking table cell styles output in ukrainian ----------");
        Path path = Paths.get(docNameUaUa);
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
    void testCheckFigureNamesUaEn() throws IOException {
        System.out.println("---------- Checking figure names doc:en, word:ua ----------");
        Path path = Paths.get(docNameUaEn);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsFiguresCheck().check(document, params, "figure");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }@Test
    void testCheckFigureNamesEnEn() throws IOException {
        System.out.println("---------- Checking figure names doc:en, word:en ----------");
        Path path = Paths.get(docNameEnEn);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("en"));
        ErrorsList errors = new ErrorsFiguresCheck().check(document, params, "figure");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckFigureNamesUaUa() throws IOException {
        System.out.println("---------- Checking figure names in doc:ua, word:ua ----------");
        Path path = Paths.get(docNameUaUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsFiguresCheck().check(document, params, "figure");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckFigureNamesEnUa() throws IOException {
        System.out.println("---------- Checking figure names in doc:ua, word:en ----------");
        Path path = Paths.get(docNameEnUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("en"));
        ErrorsList errors = new ErrorsFiguresCheck().check(document, params, "figure");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }
}
