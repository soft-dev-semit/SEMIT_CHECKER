package csit.semit.semitchecker.controller;

import csit.semit.semitchecker.errorschecking.*;
import csit.semit.semitchecker.serviceenums.Lang;
import csit.semit.semitchecker.serviceenums.MultiLang;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.io.IOException;
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

    @PostMapping(path = "/{localeInterface}/check")
    public String showErrorsShowPage(Model model,
                                     @PathVariable String localeInterface,
                                     @RequestParam String localeDoc,
                                     @RequestParam String localeWord,
                                     @RequestParam String fileForCheck,
                                     HttpServletRequest request) throws IOException {
//        System.out.println("localInterface=" + localeInterface);

        Locale locale = Lang.valueOf(localeInterface).getLocale();
        request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
        //Яка мова інтерфейсу була встановлену у MircosoftWord на компютері виконавця? Реалізовані RU, UA, EN
        Locale wordLocale= MultiLang.valueOf(localeWord).getLocale();
        //На якій мові документ? Може бути тільки дві
        Locale docLocale = Lang.valueOf(localeDoc).getLocale();
        //На якій мові показати помилки? Може бути тільки дві
        Locale interfaceLocale = Lang.valueOf(localeInterface).getLocale();
        //Створююється об'єкт із локалями для передачі в блок перевірки
        CheckParams checkParams = new CheckParams(wordLocale, docLocale, interfaceLocale);
        //TEST=====Наступний рядок - для тестового виведення. Має бути закоментований
//        String docName = "Test-file-pereliki_ru_UA.docx";
        //Наступний рядок - для "БОЙОГО" виведення
        String docName = fileForCheck;
        String errorMessage = null;
        //Завантажується файл для перевіки
        Path path = Paths.get(docName);
        DocsErrorChecker docsErrorChecker = new DocsErrorChecker(Files.newInputStream(path), docName, checkParams);
        //TEST=====Наступний рядок - для тестового виведення. Має бути закоментований
        //createTestSet(...) формує тестовий набір помилок, який показує перевірку за двома напрямками
        //docsErrorChecker.createTestSet(new Locale(localeWord), new Locale(localeDoc), locale);
        //Наступний рядок - для "БОЙОГО" виведення
        docsErrorChecker.checkDoc();
        //=======================================
        //Підготовка результатів перевірки до виведення
        List<ErrorsList> errorsList = docsErrorChecker.getChecksResults();
        List<ErrorsListDTO> errorsListsReadyToWeb = new ArrayList<>();
        if (!errorsList.isEmpty()) {
            for (ErrorsList errList: errorsList) {
                if (!errList.getErrors().isEmpty()) {
                    //Перетворення у DTO для відображення на веб-сторінці
                    ErrorsListDTO errorsListDTO = new ErrorsListDTO(checkParams.getLocaleInterface());
                    errorsListDTO.transformErrorsList(errList,true,errorMessageGetter, interfaceLocale);
                    errorsListsReadyToWeb.add(errorsListDTO);
                    //Тестове виведення у консоль - потім прибрати
//                    System.out.println("Перелік помилок: тип - "+errorsListDTO.getErrorsType());
//                    errorsListDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
                }
            }
        }
        model.addAttribute("checksResults", errorsListsReadyToWeb);
        model.addAttribute("docx name", docName);
        return "ErrorsShowPage";
    }

}
