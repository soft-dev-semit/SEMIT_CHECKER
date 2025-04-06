package csit.semit.semitchecker;

import csit.semit.semitchecker.errorschecking.CheckError;
import csit.semit.semitchecker.errorschecking.ErrorsList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ErrorProcessingTest {

    Locale localeUA = new Locale("uk","UA");
    String errorType;

    List<CheckError> errorsListAfterCheck = null;


    @Test
    void showTablesErrors() throws IOException {

        errorType = "errors-tables";
//        "Неправильне ім\\'я  таблиці"
//        "Має бути \\`Таблиця 2.1\\`"
//        "Номер таблиці не відповідає шаблону"
        ErrorsList elTableCheck = new ErrorsList(localeUA, localeUA,"errors-tables");
        elTableCheck.addError("Таблиця 1.1","bad-name");
        elTableCheck.addError("Table 2.1","bad-lang");
        elTableCheck.addError("Таблиця 2","wrong-num");
        errorsListAfterCheck = elTableCheck.getErrorList();
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
    void showBoundariesErrors() throws IOException {


    }
}
