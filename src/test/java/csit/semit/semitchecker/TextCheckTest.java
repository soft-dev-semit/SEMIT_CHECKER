package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class TextCheckTest {
    private String enDoc = "titles_layout_text_en.docx";
    private String uaDoc = "titles_layout_text_ua.docx";

    private ErrorMessageGetter errorMessageGetter;
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
                "resourcesbundles.errorstexts.text"); // без ".properties"
        messageSource.setDefaultEncoding("UTF-8");

        errorMessageGetter = new ErrorMessageGetter(messageSource);
    }

        @Test
    void testCheckTitleNamesEn() throws IOException {
        System.out.println("Checking main text in english");
        Path path = Paths.get(enDoc);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("en"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTextCheck().check(document, params, "text");
        for (CheckError error : errors.getErrors()) {
            System.out.println(error);
        }
    }

    @Test
    void testCheckTitleNamesUa() throws IOException {
        System.out.println("Checking main text in ukrainian");
        Path path = Paths.get(uaDoc);
        XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
        CheckParams params = new CheckParams();
        params.setLocaleDoc(Locale.forLanguageTag("uk"));
        params.setLocaleWord(Locale.forLanguageTag("uk"));
        ErrorsList errors = new ErrorsTextCheck().check(document, params, "text");
//        for (CheckError error : errors.getErrors()) {
//            System.out.println(error);
//
//        }
        if (!errors.getErrors().isEmpty()) {
//                        errList.getErrors().forEach(System.out::println);
            //Перетворення у DTO для відображення на веб-сторінці
            ErrorsListDTO errorsListDTO = new ErrorsListDTO(params.getLocaleInterface());
            errorsListDTO.transformErrorsList(errors, true, errorMessageGetter, params.getLocaleInterface());
//            Тестове виведення у консоль - потім прибрати
            System.out.println("Перелік помилок: тип - " + errorsListDTO.getErrorsType());
            errorsListDTO.getErrorListReadyToShow().stream().forEach(System.out::println);
        }
    }
}
