package csit.semit.semitchecker;


import csit.semit.semitchecker.docutils.CalcDocStatistic;
import csit.semit.semitchecker.docutils.DocStatistic;
//import csit.semit.semitchecker.errorschecking.ParagraphAnalizer;
//import csit.semit.semitchecker.errorschecking.ParagraphInfo;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class DocStatisticCalcTest {

    // Имя ворд-файлу для тестування обробки
//    String docName = "DRB_test_ua_UA.docx";
//    String docName = "DRB_test_ua_EN.docx";
//    String docName = "DRB_test_en_UA.docx";
//    String docName = "Test-file-pereliki.uk.en.docx";
//    String docName = "Test-file-pereliki.docx";
//String docName = "КР_МногоПомилок_ua_UA.docx";
String docName = "КР_МногоПомилок_en_UA.docx";
    String wordLocale = "EN";
    String docLocale = "UA";


    @Test
    void showDocParagraphs() throws IOException {

        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path),docName,docLocale,wordLocale);
        List<XWPFParagraph> paragraphs = calcDocStatistic.getParagraphesDoc();
        for (int i = 0; i < paragraphs.size(); i++) {
            System.out.println("Абзац " + (i + 1) + "(Стиль - "+ paragraphs.get(i).getStyle()  +"): " + paragraphs.get(i).getText());
        }
    }

    @Test
    void showDocDefStyleParagraphs() throws IOException {
        System.out.println("TEST#showDocDefStyleParagraphs");
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path),docName,docLocale,wordLocale);
        String styleName = "Tablenumber";
        List<XWPFParagraph> paragraphs = calcDocStatistic.getParagraphesDocDefStyle(styleName);
        for (int i = 0; i < paragraphs.size(); i++) {
            System.out.println("Абзац " + (i + 1) + "(Стиль - "+ paragraphs.get(i).getStyleID()  +"): " + paragraphs.get(i).getText());
        }
    }

//    @Test
//    void showParagraphsFullInfo() throws IOException {
//        Path path = Paths.get(docName);
//        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));
//        List<ParagraphInfo> paragraphInfoList = ParagraphAnalizer.analyzeParagraphs(xwpfDocument);
//        paragraphInfoList.stream().forEach(System.out::println);
//    }

    @Test
    void calcParams() throws IOException {
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        System.out.println("CountPages = "+ calcDocStatistic.getCountPages());
        System.out.println("CountFigures = "+ calcDocStatistic.getCountFigures());
        System.out.println("CountTables = "+ calcDocStatistic.getCountTables());
        System.out.println("CountSources = "+ calcDocStatistic.getCountSources());
        System.out.println("CountCountAppendixes = "+ calcDocStatistic.getCountAppendixes());

    }

    @Test
    void testPrepareAbstract() throws IOException {
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        DocStatistic statistic = calcDocStatistic.calcParam();
        System.out.println("CountPages = "+ statistic.getCountPages());
        System.out.println("CountFigures = "+ statistic.getCountFigures());
        System.out.println("CountTables = "+ statistic.getCountTables());
        System.out.println("CountSources = "+ statistic.getCountSources());
        System.out.println("CountCountAppendixes = "+ statistic.getCountAppendixes());
        System.out.println("AbstractUARow = "+ statistic.getAbstractUARow());
        System.out.println("AbstractENRow = "+ statistic.getAbstractENRow());
    }


    @Test
    void showUsedStyles() throws IOException {
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        Set<XWPFStyle> usedStyles = calcDocStatistic.getUsedStyles();
        if (usedStyles.isEmpty()) {
            System.out.println("В документе нет использованных стилей.");
        } else {
            usedStyles.forEach(style->System.out.println("ID: " + style.getStyleId() + " | Имя: " +
                    ((style != null) ? style.getName() : "Unknown")));
        }
    }

    @Test
    void showLocals() throws IOException {
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        System.out.println(calcDocStatistic.calcParam());
    }

}
