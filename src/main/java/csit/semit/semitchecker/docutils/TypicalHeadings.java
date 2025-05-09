package csit.semit.semitchecker.docutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TypicalHeadings {
    private final MessageSource messageSource;

    @Autowired
    TypicalHeadings(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // Enum для стандартних заголовків
    public enum HeadingKey {
        ABSTRACT("heading.abstract"),
        CONTENTS("heading.contents"),
        SYMBOLS_LIST("heading.symbols"),
        INTRODUCTION("heading.introduction"),
        CONCLUSIONS("heading.conclusions"),
        REFERENCES("heading.references"),
        APPENDIX("heading.appendix");

        private final String propertyKey;

        HeadingKey(String propertyKey) {
            this.propertyKey = propertyKey;
        }

        public String getPropertyKey() {
            return propertyKey;
        }
    }

    /**
     * Отримати стандартний заголовок для вказаної мови
     * @param key Ключ заголовка
     * @param language Код мови (UA/EN)
     * @return Текст заголовка
     */
    public String getHeading(HeadingKey key, String language) {
        Locale locale = new Locale(language.toLowerCase());
        return messageSource.getMessage(key.getPropertyKey(), null, locale);
    }

    /**
     * Отримати всі стандартні заголовки для вказаної мови
     * @param language Код мови (UA/EN)
     * @return Масив стандартних заголовків
     */
    public String[] getAllHeadings(String language) {
        String[] headings = new String[HeadingKey.values().length];

        int i = 0;
        for (HeadingKey key : HeadingKey.values()) {
            headings[i++] = getHeading(key, language);
        }

        return headings;
    }

    /**
     * Перевірити, чи є заголовок стандартним для вказаної мови
     * @param heading Текст заголовка для перевірки
     * @param language Код мови (UA/EN)
     * @return true, якщо це стандартний заголовок, false в іншому випадку
     */
    public boolean isStandardHeading(String heading, String language) {
        String[] headings = getAllHeadings(language);
        for (String std : headings) {
            if (heading.trim().equalsIgnoreCase(std)) {
                return true;
            }
        }
        return false;
    }
}
