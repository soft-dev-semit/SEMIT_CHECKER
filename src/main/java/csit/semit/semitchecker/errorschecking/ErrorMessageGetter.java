package csit.semit.semitchecker.errorschecking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ErrorMessageGetter {
    private final MessageSource messageSource;

    @Autowired
    public ErrorMessageGetter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    public String getMessage(String key) {
        // дефолтна локаль — українська
        return getMessage(key, new Locale("uk", "UA"));
    }
}
