package csit.semit.semitchecker.errorschecking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ErrorsList {
    private List<CheckError> errorList;
    private Locale localeDoc;
    private Locale localeWord;
    private String errorsType;

    public ErrorsList(Locale localeDoc, Locale localeWord, String errorsType) {
        this.errorList = new ArrayList<>();
        this.localeDoc = localeDoc;
        this.localeWord = localeWord;
        this.errorsType = errorsType;
    }

    public void addError(String errorPlace, String errorText) {
        if (errorList != null) {
            errorList.add(new CheckError(errorPlace, errorText));
        }
    }

    public List<CheckError> getErrorList() {
        return errorList;
    }



}
