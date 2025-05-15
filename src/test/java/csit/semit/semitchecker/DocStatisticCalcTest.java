package csit.semit.semitchecker;

import csit.semit.semitchecker.docutils.CalcDocStatistic;
import csit.semit.semitchecker.docutils.DocStatistic;
import csit.semit.semitchecker.serviceenums.PerelikType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocStatisticCalcTest {

    // Имя ворд-файлу для тестування обробки
//    String docName = "DRB_test_ua_UA.docx";
//    String docName = "DRB_test_ua_EN.docx";
//    String docName = "DRB_test_en_UA.docx";
//String docName = "КР_МногоПомилок_ua_UA.docx";
//String docName = "КР_МногоПомилок_en_UA.docx";
//String docName = "КП_АППЗ2_2025 Мелещук 2025_05_13.docx";
    String docName = "Шаталова_Blue.docx";
    //    String docName = "Document1.docx";
    String wordLocale = "UA";
    String docLocale = "UA";

    @Test
    void showDocParagraphs() throws IOException {

        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        List<XWPFParagraph> paragraphs = calcDocStatistic.getParagraphesDoc();
        for (int i = 0; i < paragraphs.size(); i++) {
            System.out.println("Абзац " + (i + 1) + "(Стиль - " + paragraphs.get(i).getStyle() + "): " + paragraphs.get(i).getText());
        }
    }

    @Test
    void showDocDefStyleParagraphs() throws IOException {
        System.out.println("TEST#showDocDefStyleParagraphs");
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        String styleName = "Tablenumber";
        List<XWPFParagraph> paragraphs = calcDocStatistic.getParagraphesDocDefStyle(styleName);
        for (int i = 0; i < paragraphs.size(); i++) {
            System.out.println("Абзац " + (i + 1) + "(Стиль - " + paragraphs.get(i).getStyleID() + "): " + paragraphs.get(i).getText());
        }
    }


    //DDE 20250517
    @Test
    void showNumericOneParagraphs() throws IOException {
        System.out.println("TEST#showNumericOneParagraphs");
        Path path = Paths.get(docName);
        XWPFDocument document = new XWPFDocument((Files.newInputStream(path)));
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
//        String styleName = PerelikType.ListNumericWithBracket.name();
        String styleName = PerelikType.ListNumeric1.name();
//        String styleName = PerelikType.ListNumericAua.name();
//        String styleName = PerelikType.ListNumericAen.name();
        XWPFNumbering numbering = document.getNumbering();
        List<XWPFParagraph> paragraphs = calcDocStatistic.getParagraphesDocDefStyle(styleName);

        for (XWPFParagraph paragraph : paragraphs) {
            BigInteger numID = paragraph.getNumID();
            BigInteger ilvl = paragraph.getNumIlvl();
            String numLevelText = paragraph.getNumLevelText();

            System.out.println("NumID: " + numID+" --- " + paragraph.getText());
//            System.out.println("Ilvl: " + ilvl);
//            System.out.println("NumLevelText: " + numLevelText);
//            System.out.println("-----");

        }
        System.out.println("\n\nAfter renumbering");

        Map<BigInteger, int[]> numberingCounters = new HashMap<>();
        for (XWPFParagraph paragraph : paragraphs) {
            BigInteger numID = paragraph.getNumID();
            if (numID != null) {
                BigInteger ilvl = paragraph.getNumIlvl();
                if (ilvl != null) {
                    int level = ilvl.intValue();
                    int[] counters = numberingCounters.get(numID);
                    if (counters == null) {
                        counters = new int[10]; // assuming max 10 levels
                        numberingCounters.put(numID, counters);
                    }
                    counters[level]++;
                    // Reset lower levels
                    for (int i = level + 1; i < counters.length; i++) {
                        counters[i] = 0;
                    }
                    // Build numbering string
                    StringBuilder numberingStr = new StringBuilder();
                    for (int i = 0; i <= level; i++) {
                        if (counters[i] == 0) break;
                        numberingStr.append(numID).append(".").append(counters[i]).append(".");
                    }
                    System.out.println(numberingStr.toString() + " Text: " + paragraph.getText());
                }
            }
        }
    }

    @Test
    void showListParagraphs() throws IOException {
        System.out.println("TEST#showListParagraphs");
        Path path = Paths.get(docName);
        XWPFDocument document = new XWPFDocument((Files.newInputStream(path)));
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        for (XWPFParagraph paragraph : paragraphs) {
            if (paragraph.getStyle()!=null) {
                PerelikType pt = PerelikType.getPerelikTypeByStyleName(paragraph.getStyle());
                if (pt!=null) {
                    BigInteger numID = paragraph.getNumID();
                    BigInteger ilvl = paragraph.getNumIlvl();
                    String numLevelText = paragraph.getNumLevelText();

                    System.out.println(pt.name().substring(4)+" -- NumID: " + numID + " --- " + paragraph.getText());
                }
            }
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
//        System.out.println("CountPages = "+ calcDocStatistic.getCountPages());
        System.out.println("CountFigures = " + calcDocStatistic.getCountFigures());
        System.out.println("CountTables = " + calcDocStatistic.getCountTables());
        System.out.println("CountSources = " + calcDocStatistic.getCountSources());
        System.out.println("CountCountAppendixes = " + calcDocStatistic.getCountAppendixes());

    }

    @Test
    void testPrepareAbstract() throws IOException {
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        DocStatistic statistic = calcDocStatistic.calcParam();
        System.out.println("CountPages = " + statistic.getCountPages());
        System.out.println("CountFigures = " + statistic.getCountFigures());
        System.out.println("CountTables = " + statistic.getCountTables());
        System.out.println("CountSources = " + statistic.getCountSources());
        System.out.println("CountCountAppendixes = " + statistic.getCountAppendixes());
        System.out.println("AbstractUARow = " + statistic.getAbstractUARow());
        System.out.println("AbstractENRow = " + statistic.getAbstractENRow());
    }


    @Test
    void showUsedStyles() throws IOException {
        Path path = Paths.get(docName);
        CalcDocStatistic calcDocStatistic = new CalcDocStatistic(Files.newInputStream(path), docName, docLocale, wordLocale);
        Set<XWPFStyle> usedStyles = calcDocStatistic.getUsedStyles();
        if (usedStyles.isEmpty()) {
            System.out.println("В документе нет использованных стилей.");
        } else {
            usedStyles.forEach(style -> System.out.println("ID: " + style.getStyleId() + " | Имя: " +
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
