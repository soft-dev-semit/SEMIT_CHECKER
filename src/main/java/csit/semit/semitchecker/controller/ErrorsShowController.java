package csit.semit.semitchecker.controller;

import csit.semit.semitchecker.docutils.DocStatistic;
import csit.semit.semitchecker.errorschecking.*;
import csit.semit.semitchecker.serviceenums.Lang;
import csit.semit.semitchecker.serviceenums.MultiLang;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class ErrorsShowController {

    @Autowired
    ErrorMessageGetter errorMessageGetter;

    @Autowired
    private ServletContext servletContext;

    @PostMapping(path = "/{localeInterface}/check")
    public String showErrorsShowPage(Model model,
                                     @PathVariable String localeInterface,
                                     @RequestParam String fileForCheck,
                                     @RequestParam String localeWord,
                                     @RequestParam String localeDoc,
                                     @RequestParam int countPages,
                                     @RequestParam int countFigures,
                                     @RequestParam int countTables,
                                     @RequestParam int countSources,
                                     @RequestParam int countAppendixes,
                                     @RequestParam String abstractUA,
                                     @RequestParam String abstractEN,
                                     @NotNull HttpServletRequest request) {
        //Яка мова інтерфейсу була встановлена у MircosoftWord на компютері виконавця? Реалізовані RU, UA, EN
//        Locale wordLocale= MultiLang.valueOf(localeWord).getLocale();
        Locale wordLocale = Locale.forLanguageTag(localeWord.replace("_", "-"));
        Locale localeWordNorm = MultiLang.getMultiLangByLocale(wordLocale).getLocale();
        //На якій мові документ? Може бути тільки дві
//        Locale docLocale = Lang.valueOf(localeDoc).getLocale();
        Locale docLocale = Locale.forLanguageTag(localeDoc.replace("_", "-"));
        Locale localeDocNorm = Lang.getLangByLocale(docLocale).getLocale();
        //На якій мові показати помилки? Може бути тільки дві
        Locale interfaceLocale = Lang.valueOf(localeInterface).getLocale();
        request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, interfaceLocale);
        //Створююється об'єкт із локалями для передачі в блок перевірки
        CheckParams checkParams = new CheckParams(localeWordNorm, localeDocNorm, interfaceLocale);

        //Завантаження та обробка файлу
        InputStream inputStreamForCheckDoc = null;
        List<ErrorsListDTO> errorsListsReadyToWeb  = null;
        try {
            //TEST=====Наступний рядок - для тестового виведення. Має бути закоментований
//        String docName = "Test-file-pereliki_ru_UA.docx";
            String uploadDir = servletContext.getRealPath("/WEB-INF/uploads/");
            //Наступний рядок - для "БОЙОГО" виведення
            String docName = uploadDir + fileForCheck;
            //Завантажується файл для перевіки
            Path path = Paths.get(docName);
            inputStreamForCheckDoc = Files.newInputStream(path);
//            System.out.println("ShowErrorsController: file for check: "+path.toAbsolutePath());
            DocsErrorChecker docsErrorChecker = new DocsErrorChecker(inputStreamForCheckDoc, docName, checkParams);
            //TEST=====Наступний рядок - для тестового виведення . Має бути закоментований
            //createTestSet(...) формує тестовий набір помилок, який показує перевірку за двома напрямками
            //docsErrorChecker.createTestSet(new Locale(localeWord), new Locale(localeDoc), locale);
            //Наступний рядок - для "БОЙОГО" виведення
            docsErrorChecker.checkDoc();
            System.out.println("ErrorsShowController: перевірка файлу "+docName+" успішно завершена");
            //=======================================
            //Підготовка результатів перевірки до виведення
            List<ErrorsList> errorsList = docsErrorChecker.getChecksResults();
            errorsListsReadyToWeb = new ArrayList<>();
            if (!errorsList.isEmpty()) {
                for (ErrorsList errList : errorsList) {
                    if (!errList.getErrors().isEmpty()) {
//                        errList.getErrors().forEach(System.out::println);
                        //Перетворення у DTO для відображення на веб-сторінці
                        ErrorsListDTO errorsListDTO = new ErrorsListDTO(checkParams.getLocaleInterface());
                        errorsListDTO.transformErrorsList(errList, true, errorMessageGetter, interfaceLocale);
                        errorsListsReadyToWeb.add(errorsListDTO);
                        //Тестове виведення у консоль - потім прибрати
//                    System.out.println("Перелік помилок: тип - "+errorsListDTO.getErrorsType());
//                    errorsListDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
                    }
                }
                model.addAttribute("noerrors", false);
            } else {
                model.addAttribute("noerrors", true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ErrorsShowController: помилка відкриття файлу для перевірки - " + e.getMessage());
            model.addAttribute("openfileForCheckProblem", true);
            model.addAttribute("checkingProblem", false);
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("ErrorsShowController: неочікувана помилка при перевірці документу - " + e.getMessage());
            model.addAttribute("fileForCheckOpenRes", false);
            model.addAttribute("checkingProblem", true);
        } finally {
            if (inputStreamForCheckDoc!=null) {
                try {
                    inputStreamForCheckDoc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //Перелік помилок (пустий чи ні) додається як атрибут
        model.addAttribute("checksResults", errorsListsReadyToWeb);
        //Параметри, що повернутись обратно на сторінку та видати дані про файл
        DocStatistic statistic = new DocStatistic();
        statistic.setFilename(fileForCheck);
        statistic.setDocLocale(Locale.forLanguageTag(localeDoc.replace("_", "-")));
        statistic.setWordLocale(Locale.forLanguageTag(localeWord.replace("_", "-")));
        statistic.setCountPages(countPages);
        statistic.setCountFigures(countFigures);
        statistic.setCountTables(countTables);
        statistic.setCountSources(countSources);
        statistic.setCountAppendixes(countAppendixes);
        statistic.setAbstractUARow(abstractUA);
        statistic.setAbstractENRow(abstractEN);
//        System.out.println("ErrorShowController#statistic = "+statistic);
        model.addAttribute("statistic", statistic);

        return "ErrorsShowPage";
    }

    @GetMapping(path = "/{localeInterface}/check")
    public String showErrorsShowPageGet(@NotNull Model model, @PathVariable String localeInterface) {
        model.addAttribute("statistic", null);
        return "redirect:/" + localeInterface + "/mainpage";
    }


}
