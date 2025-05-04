package csit.semit.semitchecker.errorschecking;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Data
public class ErrorsListDTO {
    private List<CheckErrorDTO> errorListReadyToShow;
    private String errorsType;

    private Locale localeInterface;

    public ErrorsListDTO(Locale localeInterface) {
        errorListReadyToShow = new ArrayList<>();
        this.localeInterface = localeInterface;
    }

    //Метод, що додає до переліку помилку та місце її знаходження
    public void transformErrorsList(ErrorsList errorList, boolean isNewList,
                                    ErrorMessageGetter errorMessageGetter, Locale localeInterface) {
        if (isNewList) {
            errorListReadyToShow.clear();
        }
        this.errorsType = errorList.getErrorsType();
        for (CheckError err : errorList.getErrors()) {
            String localizedMsg = errorMessageGetter.getMessage(err.getErrorCodText(), localeInterface);
            errorListReadyToShow.add(new CheckErrorDTO(err, errorsType, localizedMsg));
        }
    }

    public void clearErrorListDTO() {
        if (errorListReadyToShow != null) {
            errorListReadyToShow.clear();
        }
    }

    public List<CheckErrorDTO> getErrorListReadyToShow() {
        return errorListReadyToShow;
    }

    public String getErrorsType() {
        return errorsType;
    }

    public Locale getLocaleInterface() {
        return localeInterface;
    }
}
