package csit.semit.semitchecker.errorschecking;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;


@Getter
public class CheckErrorDTO {
    private String errorPlace;
    private String errorText;
    private String checkType;

    public CheckErrorDTO(CheckError checkError, String checkType, String localizedMessage) {
        this.errorPlace = checkError.getErrorPlace();
        //Видається повідомлення та його код
//        this.errorText = checkError.getErrorCodText() + ": " + localizedMessage;
        this.errorText = localizedMessage;
        this.checkType = checkType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("");
        sb.append("(").append(checkType).append(") ");
        sb.append(errorPlace).append(" ===> ");
        sb.append(errorText);
        return sb.toString();
    }
}
