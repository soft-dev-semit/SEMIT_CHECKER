package csit.semit.semitchecker.serviceenums;

import org.jetbrains.annotations.Nullable;

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

    public static @Nullable MultiLang getMultiLangByLocale(Locale localeIn) {
        for (MultiLang lang: MultiLang.values()) {
            if (localeIn.equals(lang.getLocale())) {
                return lang;
            }
        }
        return null;
    }
}
