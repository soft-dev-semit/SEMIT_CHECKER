package csit.semit.semitchecker.docutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TypicalHeadingStyles {
    private final MessageSource messageSource;

    @Autowired
    TypicalHeadingStyles(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    public enum StyleKey {
        HEADING_1("style.heading.first"),
        HEADING_2("style.heading.second"),
        HEADING_3("style.heading.third");

        private final String propertyKey;

        StyleKey(String propertyKey) {
            this.propertyKey = propertyKey;
        }

        public String getPropertyKey() {
            return propertyKey;
        }
    }

    public String getHeadingStyle(TypicalHeadingStyles.StyleKey key, String language) {
        Locale locale = new Locale(language.toLowerCase());
        return messageSource.getMessage(key.getPropertyKey(), null, locale);
    }

    public String[] getAllHeadingStyles(String language) {
        String[] headings = new String[TypicalHeadingStyles.StyleKey.values().length];

        int i = 0;
        for (TypicalHeadingStyles.StyleKey key : TypicalHeadingStyles.StyleKey.values()) {
            headings[i++] = getHeadingStyle(key, language);
        }

        return headings;
    }

    /**
     * Перевірити, чи є стиль заголовку стандартним для вказаної мови
     * @param heading Текст заголовка для перевірки
     * @param language Код мови (UA/EN/RU)
     * @return true, якщо це стандартний стиль заголовку, false в іншому випадку
     */
    public boolean isStandardHeadingStyle(String heading, String language) {
        String[] headings = getAllHeadingStyles(language);
        for (String std : headings) {
            if (heading.trim().equalsIgnoreCase(std)) {
                return true;
            }
        }
        return false;
    }
}
