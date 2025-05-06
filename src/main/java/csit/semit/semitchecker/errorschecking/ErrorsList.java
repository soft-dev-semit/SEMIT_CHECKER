package csit.semit.semitchecker.errorschecking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ErrorsList {
    private List<CheckError> errors;
    private Locale localeDoc;
    private Locale localeWord;
    private String errorsType;

    public ErrorsList(Locale localeWord, Locale localeDoc, String errorsType) {
        this.errors = new ArrayList<>();
        this.localeWord = localeWord;
        this.localeDoc = localeDoc;
        this.errorsType = errorsType;
    }

    //Метод, що додає до переліку помилку та місце її знаходження
    public void addError(String errorPlace, String errorText) {
        if (errors != null) {
            errors.add(new CheckError(errorPlace, errorText));
        }
    }

//    //Метод, що додає до переліку помилку (місце знаходження уже призначене в середині помилки)
//    public void addError(CheckError err) {
//        if (errorList != null) {
//            errorList.add(err);
//        }
//    }

    //Метод, який додає до поточного переліку всі помилки з іншого переліку
    public void addErrorList(ErrorsList otherErrorList) {
        if (errors != null) {
            for (CheckError checkError: otherErrorList.getErrors())
            errors.add(checkError);
        }
    }

    public void clearErrorList() {
        if (errors != null) {
            errors.clear();
        }
    }

    public List<CheckError> getErrors() {
        return errors;
    }

    public String getErrorsType() {
        return errorsType;
    }
}
