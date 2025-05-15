package csit.semit.semitchecker.serviceenums;

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

    public static Lang getLangByLocale(Locale localeIn) {
        for (Lang lang: Lang.values()) {
            if (localeIn.equals(lang.getLocale())) {
                return lang;
            }
        }
        return null;
    }
}
