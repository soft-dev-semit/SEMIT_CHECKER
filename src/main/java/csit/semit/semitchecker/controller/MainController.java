package csit.semit.semitchecker.controller;

import csit.semit.semitchecker.docutils.CalcDocStatistic;
import csit.semit.semitchecker.docutils.DocStatistic;
import csit.semit.semitchecker.serviceenums.Lang;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Controller
public class MainController {

    @GetMapping(path = "/")
    public String showInvitePage(Model model) {
        return "InvitePage";
    }

    @GetMapping(path = "/{localeInterface}/mainpage")
    public String viewIndexPage(Model model,
                                @PathVariable String localeInterface,
                                HttpServletRequest request) {
        //System.out.println("localInterface="+localInterface);

        // Встановлення локали
        Locale locale = Lang.valueOf(localeInterface).getLocale();
        HttpSession session = request.getSession();
        session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);

        DocStatistic statistic = null;
        model.addAttribute("statistic", statistic);
        model.addAttribute("uploadFileRes", null);
        return "SemitCheckerMainPage";
    }

    //Виклик сторинки "з середини" (метод POST - для повернення з перевірки
    @PostMapping(path = "/{localeInterface}/mainpage")
    public String viewIndexPagePost(Model model,
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
                                    HttpServletRequest request) {
        //System.out.println("localInterface="+localInterface);

        // Встановлення локалі інтерфейсу
        Locale locale = Lang.valueOf(localeInterface).getLocale();
        HttpSession session = request.getSession();
        session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
        //Параметри, що повернутись обратно на сторінку та видати дані про файл
        DocStatistic statistic = new DocStatistic();
        statistic.setFilename(fileForCheck);
        statistic.setWordLocale(Locale.forLanguageTag(localeWord.replace("_", "-")));
        statistic.setDocLocale(Locale.forLanguageTag(localeDoc.replace("_", "-")));
        statistic.setCountPages(countPages);
        statistic.setCountFigures(countFigures);
        statistic.setCountTables(countTables);
        statistic.setCountSources(countSources);
        statistic.setCountAppendixes(countAppendixes);
        statistic.setAbstractUARow(abstractUA);
        statistic.setAbstractENRow(abstractEN);
//        System.out.println(statistic);
        model.addAttribute("statistic", statistic);
        model.addAttribute("uploadFileRes", null);
        return "SemitCheckerMainPage";
    }

    @Autowired
    private ServletContext servletContext;

    @PostMapping("/{localeInterface}/upload")
    public String checkDocxReport(Model model,
                                  @RequestParam MultipartFile file,
                                  @PathVariable String localeInterface,
                                  @RequestParam String localeDoc,
                                  @RequestParam String localeWord,
                                  HttpServletRequest request) {
        //Завантаження файлу з диску користувача
        String docName = file.getOriginalFilename();
        DocStatistic statistic = null;
        String docLocale = localeDoc;
        String wordLocale = localeWord;
        CalcDocStatistic paramsCalc = null;
        try {
            InputStream inputStream = file.getInputStream();
            paramsCalc = new CalcDocStatistic(inputStream, docName, docLocale, wordLocale);
            statistic = paramsCalc.calcParam();
            inputStream.close();
        } catch (IOException e) {
            //TODO What It necessary to do with mistake?!
            e.printStackTrace(System.err);
            statistic.setFilename("");
        }
        //Заливка файлу на диск веб-серверу
//        // Змінна для запису результату
        boolean fileResUploadSuccess = false;
        try {
            // Отримуємо абсолютний шлях до папки для завантаження
            String uploadDir = servletContext.getRealPath("/WEB-INF/uploads/");
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs(); // Створюємо каталог, якщо він не існує
            }

            // Отримуємо ім'я файлу і формуємо шлях
            String fileName = file.getOriginalFilename();
            File destinationFile = new File(uploadDir, fileName);

            // Перезаписуємо файл, якщо він вже існує
            if (destinationFile.exists()) {
                System.err.println("MainController: файл "+destinationFile.getAbsolutePath()+" вже був завантажений ==> ,буде вилучений перед оновленням");
                destinationFile.delete(); // Видаляємо старий файл
            } else {
                System.out.println("MainController: завантажується файл: "+destinationFile.getAbsolutePath());
            }
            // Записується новий
            file.transferTo(destinationFile);
            System.out.println("MainController: файл: "+destinationFile.getAbsolutePath()+" успішно завантажений!");
            fileResUploadSuccess = true;
        } catch (IOException e) {
            System.err.println("MainController: помилка завантаження файлу: " + e.getMessage());
            e.printStackTrace();
            fileResUploadSuccess = false;
        }

        // Встановлення локали
        Locale locale = Lang.valueOf(localeInterface).getLocale();
        HttpSession session = request.getSession();
        session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);

        model.addAttribute("statistic", statistic);
        model.addAttribute("uploadFileRes", fileResUploadSuccess);
        return "SemitCheckerMainPage";
    }
    @GetMapping(path = "/{localeInterface}/upload")
    public String showErrorsShowPageGet(Model model, @PathVariable String localeInterface) {
        model.addAttribute("statistic", null);
        return "redirect:/" + localeInterface + "/mainpage";
    }

}
