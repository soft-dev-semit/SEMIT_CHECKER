package csit.semit.semitchecker.errorschecking;

import java.util.Locale;

public enum Lang {
    UA(new Locale("uk", "UA")),
    EN(Locale.ENGLISH);

    private final Locale locale;

    Lang(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}
