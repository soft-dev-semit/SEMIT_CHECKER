package csit.semit.semitchecker.errorschecking;


public class CheckError {
    private String errorPlace;

    private String errorCodText;


    public CheckError(String errorPlace, String errorCodText) {
        this.errorPlace = errorPlace;
        this.errorCodText = errorCodText;
    }

    public String getErrorPlace() {
        return errorPlace;
    }

    public String getErrorCodText() {
        return errorCodText;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CheckError{");
        sb.append("errorPlace='").append(errorPlace).append('\'');
        sb.append(", errorCodText='").append(errorCodText).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
