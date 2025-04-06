package csit.semit.semitchecker.errorschecking;

import java.util.Locale;

public enum MultiLang {
    UA(new Locale("uk", "UA")),
    EN(Locale.ENGLISH),

    RU(new Locale("ru", "RU"));

    private final Locale locale;

    MultiLang(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}
