package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.*;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class TableCheckTest {
    String docNameUaEn = "tables_tables_test_ua_en.docx";
    String docNameUaUa = "tables_tables_test_ua_ua.docx";
    String docNameEnEn = "tables_tables_test_en_en.docx";
    String docNameEnUa = "tables_tables_test_en_ua.docx";

    void showPars() throws IOException {
        Path path = Paths.get("D:\\study\\APPZ2\\Шаблон для виконання КР АППЗ.docx");
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            System.out.println(paragraph.getStyle());
        }
    }

    @Test
    void showTableCellStyles() throws IOException {
        System.out.println("---------- Checking table cell styles output in ukrainian ----------");
        Path path = Paths.get(docNameUaUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        for (XWPFTableRow row : document.getTables().get(0).getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    System.out.println(paragraph.getText() + "\tStyle : " + paragraph.getStyle());
                    for (XWPFRun run : paragraph.getRuns()) {
                        System.out.println("Color : " + run.getColor());
                    }
                }
            }
        }

    }

    @Test
    void testCheckTableNamesUaEn() throws IOException {
        System.out.println("---------- Checking table names doc:en, word:ua ----------");
        Path path = Paths.get(docNameUaEn);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTablesCheck().check(document, params, "table");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }@Test
    void testCheckTableNamesEnEn() throws IOException {
        System.out.println("---------- Checking table names doc:en, word:en ----------");
        Path path = Paths.get(docNameEnEn);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("en"));
        ErrorsList errors = new ErrorsTablesCheck().check(document, params, "table");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTableNamesUaUa() throws IOException {
        System.out.println("---------- Checking table names in doc:ua, word:ua ----------");
        Path path = Paths.get(docNameUaUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTablesCheck().check(document, params, "table");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTableNamesEnUa() throws IOException {
        System.out.println("---------- Checking table names in doc:ua, word:en ----------");
        Path path = Paths.get(docNameEnUa);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("en"));
        ErrorsList errors = new ErrorsTablesCheck().check(document, params, "table");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }
}
