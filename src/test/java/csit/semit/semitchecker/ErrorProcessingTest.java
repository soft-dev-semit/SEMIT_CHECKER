package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.*;
import csit.semit.semitchecker.serviceenums.Lang;
import csit.semit.semitchecker.serviceenums.MultiLang;
import csit.semit.semitchecker.serviceenums.PerelikType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class ErrorProcessingTest {

    String errorType;

    List<CheckError> errorsListAfterCheck = null;

    CheckParams checkParamsUA = new CheckParams();

    private ErrorMessageGetter errorMessageGetter;

    Path path;
    Locale localeWord;
    Locale localeDoc;
    Locale localeInterface;
    CheckParams checkParams;

    // Имя ворд-файлу для тестування обробки
//    String docName = "Test-file-pereliki_ru_UA.docx";
//    String docName = "Test-file-pereliki_en_UA.docx";
//    String docName = "Test-file-pereliki_en_EN.docx";
//    String docName = "Test-file-pereliki_en_UA.docx";
//    String docName = "КР_МногоПомилок_ua_UA.docx";
//    String docName = "КП_АППЗ2_2025 Мелещук 2025_05_13дд.docx";
//    String docName = "Шаталова_Blue.docx";
//    String docName = "ExplNote_TEST.docx";
    String docName = "КП_АППЗ2_2025 Мелещук 2025_05_13дд.docx";
//    String docName = "Шаталова_Blue.docx";


    @BeforeEach
    public void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("resourcesbundles/interfaces/mainpage-labels",
                "resourcesbundles.interfaces.errorspage-labels",
                "resourcesbundles.docskeywords.docskeywords",
                "resourcesbundles.docstyles.docswordstyles",
                "resourcesbundles/errorstexts/pereliki",
                "resourcesbundles/errorstexts/table",
                "resourcesbundles/errorstexts/figure",
                "resourcesbundles.errorstexts.titles",
                "resourcesbundles.errorstexts.layout",
                "resourcesbundles.errorstexts.text" ); // без ".properties"
        messageSource.setDefaultEncoding("UTF-8");

        errorMessageGetter = new ErrorMessageGetter(messageSource);

        //Завантажується файл для перевіки
        path = Paths.get(docName);
        //Яка мова інтерфейсу була встановлену у MircosoftWord на компютері виконавця? Реалізовані RU, UA, EN
        localeWord = Locale.forLanguageTag("uk_UA".replace("_", "-"));
//        localeWord = Locale.forLanguageTag("en".replace("_", "-"));
        Locale localeWordNorm = MultiLang.getMultiLangByLocale(localeWord).getLocale();
//        localeWord = MultiLang.UA.getLocale();
        //На якій мові документ? Може бути тільки дві
        localeDoc = Locale.forLanguageTag("uk_UA".replace("_", "-"));
//        localeDoc = Locale.forLanguageTag("en".replace("_", "-"));
        Locale localeDocNorm = Lang.getLangByLocale(localeDoc).getLocale();
//        localeDoc = Lang.UA.getLocale();
        //На якій мові показати помилки? Може бути тільки дві
        localeInterface = Lang.UA.getLocale();
//        localeInterface = Lang.EN.getLocale();
        //Створююється об`єкт із локалями для передачі в блок перевірки
        checkParams = new CheckParams(localeWordNorm, localeDocNorm, localeInterface);
    }


    @Test
    void showTablesErrors() throws IOException {

        errorType = "errors-tables";
//        "Неправильне ім\\'я  таблиці"
//        "Має бути \\`Таблиця 2.1\\`"
//        "Номер таблиці не відповідає шаблону"
        ErrorsList elTableCheck = new ErrorsList(checkParams.localeWord, checkParams.localeDoc, "errors-tables");
        elTableCheck.addError("Таблиця 1.1", "bad-name");
        elTableCheck.addError("Table 2.1", "bad-lang");
        elTableCheck.addError("Таблиця 2", "wrong-num");
        errorsListAfterCheck = elTableCheck.getErrors();
        errorsListAfterCheck.stream().forEach(System.out::println);

    }

//    @Test
//    void showTablesErrorsLocale() throws IOException {
//
//        errorType = "errors-tables";
////        "Неправильне ім\\'я  таблиці"
////        "Має бути \\`Таблиця 2.1\\`"
////        "Номер таблиці не відповідає шаблону"
//        ErrorsList elTableCheck = new ErrorsList(localeUA,localeUA,"errors-tables");
//        elTableCheck.addError("Таблиця 1.1","bad-name");
//        elTableCheck.addError("Table 2.1","bad-lang");
//        elTableCheck.addError("Таблиця 2","wrong-num");
//        errorsListAfterCheck = elTableCheck.getErrorList();
//        errorsListAfterCheck.stream().forEach(System.out::println);
//
//    }

    @Test
    void testCheckMarkedStd() throws IOException {
        //ddimae72
        path = Paths.get(docName);
        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));
        ErrorsPerelikiCheck errPerCheck = new ErrorsPerelikiCheck();
        int perelikCount = 1;
        int startParagraph = 0;
        ErrorsPerelikiCheck.Perelik p = null;
        do {
            //Тут визначається локаль
            p = errPerCheck.foundPerelik(PerelikType.ListMarkedSTD, startParagraph, xwpfDocument, MultiLang.RU.getLocale());
//            System.out.println("startParagraph == "+startParagraph);
            if (p != null) {
                System.out.println("\nMarked STD List # " + perelikCount);
                System.out.println("Placed at " + p.getPerelikPlace());
                XWPFParagraph paragraphBefore = null;
                paragraphBefore = p.getParagraphBefore();
                if (paragraphBefore != null) {
                    System.out.println(p.getParagraphBefore().getStyle() + ": " + p.getParagraphBefore().getText());
                } else {
                    System.out.println("Абзац перед переліком відсутній");
                }
                for (XWPFParagraph paragraph : p.getPerelikItems()) {
                    System.out.println(paragraph.getStyle() + ": " + paragraph.getText());
                }
                XWPFParagraph paragraphAfter = null;
                paragraphAfter = p.getParagraphAfter();
                if (paragraphAfter != null) {
                    System.out.println(paragraphAfter.getStyle() + ": " + paragraphAfter.getText());
                } else {
                    System.out.println("Абзац відразу після переліку відсутній");
                }
                XWPFParagraph paragraphAfter2 = null;
                paragraphAfter2 = p.getParagraphAfter2();
                if (paragraphAfter2 != null) {
                    System.out.println(paragraphAfter2.getStyle() + ": " + paragraphAfter2.getText());
                } else {
                    System.out.println("Абзац через рядок після переліку відсутній");
                }
                //Перевірка абзаців
                p = errPerCheck.checkPerelik(p, checkParamsUA, "errors-tables");
                if (p.getErrorsList().getErrors().size() > 0) {
                    System.out.println("ЗНАЙДЕНІ ПОМИЛКИ:");
                    for (CheckError checkError : p.getErrorsList().getErrors()) {
                        System.out.println(checkError);
                    }
                } else {
                    System.out.println("!!! === ПОМИЛКИ ВІДСУТНІ === !!!");
                }
                //Початок пошуку наступного переліку встановлюється на наступний абзац після переліку
                //якщо не знайдений кінець тексту
//                System.out.println("p.getPosStartList() = "+p.getPosStartList());
//                System.out.println("p.getPerelikItems().size() = "+ p.getPerelikItems().size());
                if ((p.getPosStartList() + p.getPerelikItems().size()) < xwpfDocument.getParagraphs().size()) {
                    startParagraph = p.getPosStartList() + p.getPerelikItems().size();
//                    System.out.println("Пошук продовжиться з абзацу "+(startParagraph+1));
                } else {
                    p = null;
//                    System.out.println("Пошук закінчений ");
                }
                perelikCount++;
            }

        } while (p != null);

    }


    //    @Autowired
//    ErrorMessageGetter errorMessageGetter;
    @Test
    void testCheckMarkedStd2() throws IOException {
        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));

        //Створюєтся перелік для зберігання помилок
        ErrorsPerelikiCheck errPerCheck = new ErrorsPerelikiCheck();

        //Визначається тип перевірки
        String typeErrors = "pereliki-check";

        //GOOOOOO!!!!
        ErrorsList errMarkedSTD = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        errMarkedSTD = errPerCheck.checkPereliksOfType(xwpfDocument, checkParams, errMarkedSTD, typeErrors, PerelikType.ListMarkedSTD);
        System.out.println("Помилки перевірки маркированих переліків:");
        if (!errMarkedSTD.getErrors().isEmpty()) {
            errMarkedSTD.getErrors().stream().forEach(
                    checkError -> {
                        String errorPlace = checkError.getErrorPlace();
                        String errorMessage = checkError.getErrorCodText() + ": "
                                + errorMessageGetter.getMessage(checkError.getErrorCodText(), checkParams.getLocaleInterface());
                        String normalMsgText = errorPlace + " === " + errorMessage;
                        System.out.println(normalMsgText);
                    });
        } else {
            System.out.println(errorMessageGetter.getMessage("pereliki.noerrors", checkParams.getLocaleInterface()));
        }

        ErrorsList errNumericWithBracket = new ErrorsList(checkParams.localeDoc, checkParams.getLocaleWord(), typeErrors);
        errNumericWithBracket = errPerCheck.checkPereliksOfType(xwpfDocument, checkParams, errNumericWithBracket, typeErrors, PerelikType.ListNumericWithBracket);
        System.out.println("Помилки перевірки нумерованих переліків - 1),2),3)...:");
        if (!errNumericWithBracket.getErrors().isEmpty()) {
            errNumericWithBracket.getErrors().stream().forEach(
                    checkError -> {
                        String errorPlace = checkError.getErrorPlace();
                        String errorMessage = checkError.getErrorCodText() + ": "
                                + errorMessageGetter.getMessage(checkError.getErrorCodText(), checkParams.getLocaleInterface());
                        String normalMsgText = errorPlace + " === " + errorMessage;
                        System.out.println(normalMsgText);
                    });
        } else {
            System.out.println(errorMessageGetter.getMessage("pereliki.noerrors", checkParams.getLocaleInterface()));
        }
    }

    @Test
    void testCheckMarkedStd3() throws IOException {
        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));
//
        //Створюєтся перелік для зберігання помилок
        ErrorsPerelikiCheck errPerCheck = new ErrorsPerelikiCheck();

        //Визначається тип перевірки
        String typeErrors = "pereliki-check";

        //GOOOOOO!!!!
        System.out.println("\nПОМИЛКИ ПЕРЕВІРКИ ПЕРЕЛІКІВ: " + typeErrors);
        ErrorsList errPereliki = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        //Проверка всіх типів переліків, визначених в enum PerelikType
//        System.out.println("\nCHECKING PERELIKS ---  " + PerelikType.ListMarkedSTD);
//        errPereliki = errPerCheck.checkPereliksOfType(xwpfDocument, checkParams, errPereliki, typeErrors, PerelikType.ListMarkedSTD);
//        System.out.println("\nCHECKING PERELIKS ---  " + PerelikType.ListNumericWithBracket);
//        errPereliki = errPerCheck.checkPereliksOfType(xwpfDocument, checkParams, errPereliki, typeErrors, PerelikType.ListNumericWithBracket);
        for (PerelikType type : PerelikType.values()) {
            System.out.println("CHECKING PERELIKS ---  " + type + "............");
            errPereliki = errPerCheck.checkPereliksOfType(xwpfDocument, checkParams, errPereliki, typeErrors, type);
        }
        System.out.println("\nРЕЗУЛЬТАТИ ПЕРЕВІРКИ: " + typeErrors);
        if (!errPereliki.getErrors().isEmpty()) {
            ErrorsListDTO errPerelikiDTO = new ErrorsListDTO(checkParams.getLocaleInterface());
            errPerelikiDTO.transformErrorsList(errPereliki, true, errorMessageGetter, localeInterface);
            errPerelikiDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
        } else {
            System.out.println(errorMessageGetter.getMessage("pereliki.noerrors", checkParams.getLocaleInterface()));
        }


    }

    @Test
    void testCheck() throws IOException {
        //Создается фабрика для проверки
        DocsErrorChecker docsErrorChecker = new DocsErrorChecker(Files.newInputStream(path), docName, checkParams);
        //Запуск проверки і повернення результатів
        docsErrorChecker.checkDoc();
        List<ErrorsList> errorsList = docsErrorChecker.getChecksResults();

        System.out.println("\nРЕЗУЛЬТАТИ ПЕРЕВІРКИ:");
        if (!errorsList.isEmpty()) {
            for (ErrorsList errList : errorsList) {
                if (!errList.getErrors().isEmpty()) {
                    System.out.println("Перелік помилок: тип - " + errList.getErrorsType());
                    ErrorsListDTO errorsListDTO = new ErrorsListDTO(checkParams.getLocaleInterface());
                    errorsListDTO.transformErrorsList(errList, true, errorMessageGetter, localeInterface);
                    errorsListDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
                }
            }
        } else {
            System.out.println(errorMessageGetter.getMessage("pereliki.noerrors", checkParams.getLocaleInterface()));
        }

    }

    @Test
    void testShowResultsTestSet() throws IOException {
        //Создается фабрика для проверки
        DocsErrorChecker docsErrorChecker = new DocsErrorChecker(Files.newInputStream(path), docName, checkParams);
        //!!!!!!!******!!!!!!!*******!!!!!!!!!!!!
        //Запуск заповнення тестового набору результатів
        docsErrorChecker.createTestSet(localeWord, localeDoc, localeInterface);
        List<ErrorsList> errorsList = docsErrorChecker.getChecksResults();

        System.out.println("\nРЕЗУЛЬТАТИ ПЕРЕВІРКИ:");
        if (!errorsList.isEmpty()) {
            for (ErrorsList errList : errorsList) {
                if (!errList.getErrors().isEmpty()) {
                    ErrorsListDTO errorsListDTO = new ErrorsListDTO(checkParams.getLocaleInterface());
                    errorsListDTO.transformErrorsList(errList, true, errorMessageGetter, localeInterface);
                    System.out.println("Перелік помилок: тип - " + errorsListDTO.getErrorsType());
                    errorsListDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
                }
            }
        } else {
            System.out.println(errorMessageGetter.getMessage("pereliki.noerrors", checkParams.getLocaleInterface()));
        }

    }

//    TODO Перевірка "потенційних" переліків
//    @Test
//    void testFindPossibleLists() throws IOException {
//        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));
//        //Создается фабрика для проверки
//        ErrorsPerelikiCheck errorsPerelikiCheck = new ErrorsPerelikiCheck();
//
//        ErrorsList errorsList = errorsPerelikiCheck.checkBadPereliks(xwpfDocument, checkParams, "pereliki");
//        System.out.println("\nРЕЗУЛЬТАТИ ПЕРЕВІРКИ:");
//        if (!errorsList.getErrors().isEmpty()) {
//            ErrorsListDTO errorsListDTO = new ErrorsListDTO(checkParams.getLocaleInterface());
//            errorsListDTO.transformErrorsList(errorsList, true, errorMessageGetter, localeInterface);
//            System.out.println("Перелік помилок: тип - " + errorsListDTO.getErrorsType());
//            errorsListDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
//        } else {
//            System.out.println(errorMessageGetter.getMessage("pereliki.noerrors", checkParams.getLocaleInterface()));
//        }
//    }

    //Перевірка методу для пояднення декількох параграфів в один. Для ListNumeric1
    @Test
    void testReplaceText() throws IOException {
        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));
        //Создается фабрика для проверки
        ErrorsPerelikiCheck errorsPerelikiCheck = new ErrorsPerelikiCheck();
        System.out.println(errorsPerelikiCheck.joinParagraphs(xwpfDocument.getParagraphs(),310,4).getText());
    }

    @Test
    void testDefСountSentenceNumeric1() throws IOException {
        XWPFDocument xwpfDocument = new XWPFDocument(Files.newInputStream(path));
        List<XWPFParagraph> paragraphs = xwpfDocument.getParagraphs();
        for (XWPFParagraph par: paragraphs) {
            int count=0;
            if (par.getText()!=null && par.getStyle()!=null) {
                if (par.getStyle().equals(PerelikType.ListNumeric1.name())) {
                    count = ErrorsPerelikiCheck.countSentences(par.getText());
                    System.out.println("Numeric1 item (count ="+count+"):"+ par.getText());
                }
            }
//            System.out.println("Numeric1 item (count ="+count+"):"+ par.getText());
        }

    }



}
