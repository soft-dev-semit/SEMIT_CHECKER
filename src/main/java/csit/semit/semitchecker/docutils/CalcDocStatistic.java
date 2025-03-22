package csit.semit.semitchecker.docutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.*;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CalcDocStatistic {

    //Документ
    private XWPFDocument document;
    //Имя файла (бо document -  це байти файлу, фактично - розборка файлу)
    private String docName;
    //мова тексту
    private String docLocale;

    public CalcDocStatistic(InputStream inputStream, String docName, String locale) throws IOException {
        document = new XWPFDocument(inputStream);
        this.docName = docName;
        this.document = document;
        this.docLocale = locale;
    }

    public DocStatistic calcParam() {
        DocStatistic res = new DocStatistic();
        res.setFilename(docName);
        res.setDocLocale(docLocale);
        res.setCountPages(this.getCountPages());
        res.setCountFigures(this.getCountFigures());
        res.setCountTables(this.getCountTables());
        res.setCountSources(this.getCountSources());
        res.setCountAppendixes(this.getCountAppendixes());
        res.prepareAbstract();
        return res;
    }


    //ОТСТУПЫ СТРАНИЦЫ

//    var sectPtr = document.getDocument().getBody().getSectPr();
//        if (sectPtr == null) {
//        errorsCollector.addError(ErrorTemplates.sectPtrNotFound());
//        return;
//    }
//var margins = sectPtr.getPgMar();
//        if (margins == null) {
//        errorsCollector.addError(ErrorTemplates.marginsNotFound());
//        return;
//    }
//
//    BigInteger leftMargin = (BigInteger) margins.getLeft();
//    BigInteger rightMargin = (BigInteger) margins.getRight();
//    BigInteger topMargin = (BigInteger) margins.getTop();
//    BigInteger bottomMargin = (BigInteger) margins.getBottom();

    public List<XWPFParagraph> getParagraphesDoc() {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs;
    }

    public List<XWPFParagraph> getParagraphesDocDefStyle(String styleName) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        List<XWPFParagraph> defParagraphs = new ArrayList<>();
        for (XWPFParagraph p : paragraphs) {
            if (p.getStyle() != null && p.getStyle().equals(styleName)) {
                defParagraphs.add(p);
            }
        }
        return defParagraphs;
    }


    public int getCountPages() {
        int count = -1;
        //TODO ПРОВЕРИТь НА СЕРВЕРЕ!!!!
        //TODO Будет ли работать без libreoffice
        File inputFile = new File(docName);
        File outputFile = new File(docName.replace(".docx", ".pdf"));

        // Запускаем LibreOffice для конвертации
        var officeManager = LocalOfficeManager.install();
        try {
            officeManager.start();

            // Конвертация .docx → .pdf
            LocalConverter.builder()
                    .officeManager(officeManager)
                    .build()
                    .convert(inputFile)
                    .to(outputFile)
                    .as(DefaultDocumentFormatRegistry.PDF)
                    .execute();

            System.out.println("Конвертация завершена ... ");

        } catch (OfficeException e) {
            throw new RuntimeException("Ошибка при конвертации в pdf", e);
        } finally {
            try {
                officeManager.stop();
            } catch (OfficeException ignored) {
            }
        }

        // Получаем количество страниц в PDF
        //!!! Но додати треба одинцю, бо чистий файл без титулок починається із сторінки 2
        try (PDDocument document = PDDocument.load(outputFile)) {
            count = document.getNumberOfPages()+1;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении PDF", e);
        }

        return count;
    }

    public int getCountFigures() {
        int count = -1;
        // Получаем все изображения в документе
        //TODO Проблема будет для рисунков, которые находятся в таблице
        List<XWPFPictureData> pictures = document.getAllPictures();
        List<XWPFChart> charts = document.getCharts();
        count = pictures.size() + charts.size();
        return count;
    }

    public int getCountTables() {
        int count = -1;
        // Получаем список таблиц
        //Если таблица разделяется на две и более частей все они считаются отдельно!!!
        List<XWPFTable> tables = document.getTables();
        count = tables.size();
        //Если таблица разделяется на две и более частей все они считаются отдельно!!!
        //Что выдать реальное, выполняется проход по заголовкам таблицы
        String styleName = "Tablenumber";
        List<XWPFParagraph> paragraphs = this.getParagraphesDocDefStyle(styleName);
        if (paragraphs.size() > 0) {
            for (int i=0; i < paragraphs.size(); i++) {
                if (paragraphs.get(i).getText().toUpperCase().startsWith("КІНЕЦЬ ТАБЛИЦІ") ||
                        paragraphs.get(i).getText().toUpperCase().startsWith("ПРОДОВЖЕННЯ ТАБЛИЦІ")) {
                    count--;
                }
            }
        }
        return count;
    }

    public int getCountSources() {
        int count = -1;
        //Найти абзац 'Список джерел інформації' із стилем 'header 1'
        //Краще перетворити у всі прописні
        String styleName = "1";
        List<XWPFParagraph> paragraphs = getParagraphesDoc();
        String etalonReferences = "Список джерел інформації".toUpperCase();
        int posSources = -1;
        int i = 0;
        if (paragraphs.size() > 0) {
            for (; i < paragraphs.size(); i++) {
                if ((paragraphs.get(i).getStyle() != null) && paragraphs.get(i).getStyle().equals(styleName)) {
                    String textP = paragraphs.get(i).getText().toUpperCase();
                    if (textP.equals(etalonReferences)) {
                        posSources = i + 1;
                        break;
                    }
                }
            }
        }
        //Получить нумерованный список после данного абзаца
        //1 Це може бути Numeric1 для моего шаблона
        //2 Нумерованный стандартный, если руками форматировано
        //3 ОБЫЧНЫЙ, в котором руками проставлены цифры,
        // но стиль "Нормал" показывается .... как null!!!!
        // як ще студент може отформатировать список?

        //
        if (posSources > -1) {
            for (i = posSources; i < paragraphs.size(); i++) {
                if ((paragraphs.get(i).getStyle() != null) && !paragraphs.get(i).getStyle().equals(styleName)) {
                    count++;
                } else {
                    break;
                }
            }
            //НО ПЕРЕЛІК може бути останній....
            //Но если есть, то i>-1
            //Количество источников равно кол-ву елементов
            //Но начальное значение -1, поэтому для нормального отображение нужно добавить 1
            count++;


        }



        return count;
    }

    public int getCountAppendixes() {
        //count = -1 - це буде ознакою порушень структури
        // В даному випадку - немає ЗМІСТ або неправильне форматування заголовків
        int count = -1;
        //Получить список заголовков1
        String styleName = "1";
        List<XWPFParagraph> paragraphs = getParagraphesDocDefStyle(styleName);
        //Найти абзац 'Список джерел інформації'
        //Краще перетворити у всі прописні
        //Все Заголовок1 после него - ДОДАТКИ!!!
        String etalonReferences = "Список джерел інформації".toUpperCase();
        if (paragraphs.size() > 0) {
            int i = 0;
            for (i = paragraphs.size() - 1; i > -1; i--) {
                String textP = paragraphs.get(i).getText().toUpperCase();
                if (textP.equals(etalonReferences)) {
                    break;
                } else {
                    count++;
                }
            }
            //НО ТАКОГО ЗАГОЛОВКА МОЖЕТ НЕ БЫТЬ
            //Но если есть, то i>-1
            if (i > -1) {
                count++;
            }
        }
        return count;
    }

    public Set<XWPFStyle> getUsedStyles() {
        XWPFStyles styles = document.getStyles();

        Set<XWPFStyle> usedStyles = new HashSet<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String styleId = paragraph.getStyle();
            if (styleId != null && styles != null) {
                XWPFStyle style = styles.getStyle(styleId);
                usedStyles.add(style);
            }
        }
        return usedStyles;
    }


}
