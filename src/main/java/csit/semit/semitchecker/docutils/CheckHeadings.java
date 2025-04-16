package csit.semit.semitchecker.docutils;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.context.MessageSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Component
public class CheckHeadings {

    private final MessageSource messageSource;
    private final TypicalHeadings typicalHeadings;

    // Мапа стилів заголовків за локаллю Office
    private Map<String, Map<Integer, String>> headingStyles = new HashMap<>();

    @Autowired
    public CheckHeadings(MessageSource messageSource, TypicalHeadings typicalHeadings) {
        this.messageSource = messageSource;
        this.typicalHeadings = typicalHeadings;
        initializeHeadingStyles();
    }

    private void initializeHeadingStyles() {
        // UA Office locale heading styles
        Map<Integer, String> uaStyles = new HashMap<>();
        uaStyles.put(1, "Заголовок 1");
        uaStyles.put(2, "Заголовок 2");
        uaStyles.put(3, "Заголовок 3");
        headingStyles.put("UA", uaStyles);

        // EN Office locale heading styles
        Map<Integer, String> enStyles = new HashMap<>();
        enStyles.put(1, "Heading 1");
        enStyles.put(2, "Heading 2");
        enStyles.put(3, "Heading 3");
        headingStyles.put("EN", enStyles);

        // RU Office locale heading styles
        Map<Integer, String> ruStyles = new HashMap<>();
        ruStyles.put(1, "Заголовок 1");
        ruStyles.put(2, "Заголовок 2");
        ruStyles.put(3, "Заголовок 3");
        headingStyles.put("RU", ruStyles);
    }

    /**
     * Отримати повідомлення про помилку з файлу .properties
     * @param key Ключ повідомлення
     * @param uiLanguage Мова інтерфейсу
     * @param args Аргументи для повідомлення
     * @return Локалізоване повідомлення
     */
    private String getMessage(String key, String uiLanguage, Object... args) {
        Locale locale = new Locale(uiLanguage.toLowerCase());
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Перевірка наявності всіх обов'язкових структурних елементів
     * @param document Документ XWPFDocument для перевірки
     * @param docLanguage Мова документа (UA/EN)
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @return Список знайдених помилок
     */
    public List<String> checkRequiredSections(XWPFDocument document, String docLanguage, String uiLanguage) {
        List<String> errors = new ArrayList<>();
        String[] requiredSections = typicalHeadings.getAllHeadings(docLanguage);

        // Вилучити всі заголовки з документа
        List<String> documentHeadings = new ArrayList<>();
        for (XWPFParagraph para : document.getParagraphs()) {
            if (para.getStyle() != null && para.getStyle().startsWith(headingStyles.get(docLanguage).get(1))) {
                documentHeadings.add(para.getText());
            }
        }

        // Перевірка відсутніх розділів
        for (String requiredSection : requiredSections) {
            boolean found = false;
            for (String heading : documentHeadings) {
                if (heading.trim().equalsIgnoreCase(requiredSection)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errors.add(getMessage("error.missing.section", uiLanguage, requiredSection));
            }
        }

        return errors;
    }

    /**
     * Перевірка наявності пустих рядків до та після заголовків
     * @param document Документ XWPFDocument для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список знайдених помилок
     */
    public List<String> checkHeadingSpacing(XWPFDocument document, String uiLanguage, String officeLocale) {
        List<String> errors = new ArrayList<>();
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);

            // Перевірка, чи є параграф заголовком
            boolean isHeading = false;
            int headingLevel = 0;

            if (para.getStyle() != null) {
                for (int level = 1; level <= 3; level++) {
                    if (para.getStyle().equals(headingStyles.get(officeLocale).get(level))) {
                        isHeading = true;
                        headingLevel = level;
                        break;
                    }
                }
            }

            if (isHeading) {
                // Перевірка наявності пустого рядка перед заголовком (окрім першого параграфа)
                if (i > 0) {
                    XWPFParagraph prevPara = paragraphs.get(i - 1);
                    if (!prevPara.getText().trim().isEmpty()) {
                        errors.add(getMessage("error.no.empty.line.before", uiLanguage, para.getText()));
                    }
                }

                // Перевірка наявності пустого рядка після заголовка (окрім останнього параграфа)
                if (i < paragraphs.size() - 1) {
                    XWPFParagraph nextPara = paragraphs.get(i + 1);
                    if (!nextPara.getText().trim().isEmpty()) {
                        errors.add(getMessage("error.no.empty.line.after", uiLanguage, para.getText()));
                    }
                }

                // Для заголовків 1-го рівня перевірка, чи є більше одного рядка тексту на сторінці
                if (headingLevel == 1 && i < paragraphs.size() - 2) {
                    // Це спрощено - у реальній реалізації потрібно перевіряти,
                    // чи знаходяться ці параграфи на одній сторінці
                    boolean hasMoreThanOneLine = false;
                    for (int j = i + 1; j < i + 3 && j < paragraphs.size(); j++) {
                        if (!paragraphs.get(j).getText().trim().isEmpty()) {
                            hasMoreThanOneLine = true;
                            break;
                        }
                    }
                    if (!hasMoreThanOneLine) {
                        errors.add(getMessage("error.not.enough.text.after.heading", uiLanguage, para.getText()));
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Перевірка порядку заголовків (1, 1.1, 1.1.1 тощо)
     * @param document Документ XWPFDocument для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список знайдених помилок
     */
    public List<String> checkHeadingOrder(XWPFDocument document, String uiLanguage, String officeLocale) {
        List<String> errors = new ArrayList<>();

        int currentChapter = 0;
        int currentSection = 0;
        int currentSubsection = 0;

        for (XWPFParagraph para : document.getParagraphs()) {
            if (para.getStyle() == null) continue;

            String text = para.getText().trim();

            // Пропустити ненумеровані заголовки, як ABSTRACT, CONTENTS тощо
            if (text.equals(text.toUpperCase()) && !text.matches("^\\d+.*")) {
                continue;
            }

            // Перевірка нумерованих заголовків (1, 1.1, 1.1.1)
            if (para.getStyle().equals(headingStyles.get(officeLocale).get(1))) {
                // Заголовок розділу (1, 2, 3...)
                if (text.matches("^\\d+\\s+.*")) {
                    try {
                        int chapterNum = Integer.parseInt(text.split("\\s+")[0]);
                        if (chapterNum != currentChapter + 1) {
                            errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                        }
                        currentChapter = chapterNum;
                        currentSection = 0;
                        currentSubsection = 0;
                    } catch (NumberFormatException e) {
                        errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                    }
                }
            } else if (para.getStyle().equals(headingStyles.get(officeLocale).get(2))) {
                // Заголовок підрозділу (1.1, 1.2, 2.1...)
                if (text.matches("^\\d+\\.\\d+\\s+.*")) {
                    String[] parts = text.split("\\s+")[0].split("\\.");
                    try {
                        int chapterNum = Integer.parseInt(parts[0]);
                        int sectionNum = Integer.parseInt(parts[1]);

                        if (chapterNum != currentChapter) {
                            errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                        } else if (sectionNum != currentSection + 1) {
                            errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                        }

                        currentSection = sectionNum;
                        currentSubsection = 0;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                    }
                }
            } else if (para.getStyle().equals(headingStyles.get(officeLocale).get(3))) {
                // Заголовок пункту (1.1.1, 1.1.2, 2.1.1...)
                if (text.matches("^\\d+\\.\\d+\\.\\d+\\s+.*")) {
                    String[] parts = text.split("\\s+")[0].split("\\.");
                    try {
                        int chapterNum = Integer.parseInt(parts[0]);
                        int sectionNum = Integer.parseInt(parts[1]);
                        int subsectionNum = Integer.parseInt(parts[2]);

                        if (chapterNum != currentChapter || sectionNum != currentSection) {
                            errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                        } else if (subsectionNum != currentSubsection + 1) {
                            errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                        }

                        currentSubsection = subsectionNum;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Перевірка форматування заголовків розділів (ВЕЛИКІ ЛІТЕРИ, напівжирний, по центру або з абзацу, без крапки в кінці)
     * @param document Документ XWPFDocument для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список знайдених помилок
     */
    public List<String> checkChapterFormatting(XWPFDocument document, String uiLanguage, String officeLocale) {
        List<String> errors = new ArrayList<>();

        for (XWPFParagraph para : document.getParagraphs()) {
            if (para.getStyle() != null && para.getStyle().equals(headingStyles.get(officeLocale).get(1))) {
                String text = para.getText().trim();

                // Перевірка на ВЕЛИКІ ЛІТЕРИ
                if (!text.equals(text.toUpperCase())) {
                    errors.add(getMessage("error.chapter.not.uppercase", uiLanguage, text));
                }

                // Перевірка на напівжирний шрифт
                boolean isBold = false;
                for (XWPFRun run : para.getRuns()) {
                    if (run.isBold()) {
                        isBold = true;
                        break;
                    }
                }
                if (!isBold) {
                    errors.add(getMessage("error.chapter.not.bold", uiLanguage, text));
                }

                // Перевірка вирівнювання
                if (para.getAlignment() != ParagraphAlignment.CENTER &&
                        para.getAlignment() != ParagraphAlignment.LEFT &&
                        para.getIndentationFirstLine() == 0) {
                    errors.add(getMessage("error.chapter.wrong.alignment", uiLanguage, text));
                }

                // Перевірка на крапку в кінці
                if (text.endsWith(".")) {
                    errors.add(getMessage("error.chapter.has.period", uiLanguage, text));
                }
            }
        }

        return errors;
    }

    /**
     * Перевірка форматування заголовків підрозділів (малі літери з першої великої, напівжирний, з абзацу, без крапки в кінці)
     * @param document Документ XWPFDocument для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список знайдених помилок
     */
    public List<String> checkSubsectionFormatting(XWPFDocument document, String uiLanguage, String officeLocale) {
        List<String> errors = new ArrayList<>();

        for (XWPFParagraph para : document.getParagraphs()) {
            if (para.getStyle() != null &&
                    (para.getStyle().equals(headingStyles.get(officeLocale).get(2)) ||
                            para.getStyle().equals(headingStyles.get(officeLocale).get(3)))) {

                String text = para.getText().trim();
                String[] words = text.split("\\s+");

                // Пропустити частину з нумерацією
                int startWordIndex = 0;
                if (words.length > 0 && words[0].matches("\\d+(\\.\\d+)*")) {
                    startWordIndex = 1;
                }

                // Перевірка першої великої літери, решти малих
                if (startWordIndex < words.length) {
                    String firstWord = words[startWordIndex];
                    if (firstWord.length() > 0) {
                        char firstChar = firstWord.charAt(0);
                        if (!Character.isUpperCase(firstChar)) {
                            errors.add(getMessage("error.subsection.first.not.uppercase", uiLanguage, text));
                        }

                        // Перевірка, чи решта заголовка в усіх ВЕЛИКИХ літерах
                        boolean allUppercase = true;
                        for (int i = startWordIndex; i < words.length; i++) {
                            if (!words[i].equals(words[i].toUpperCase())) {
                                allUppercase = false;
                                break;
                            }
                        }
                        if (allUppercase) {
                            errors.add(getMessage("error.subsection.not.lowercase", uiLanguage, text));
                        }
                    }
                }

                // Перевірка на напівжирний шрифт
                boolean isBold = false;
                for (XWPFRun run : para.getRuns()) {
                    if (run.isBold()) {
                        isBold = true;
                        break;
                    }
                }
                if (!isBold) {
                    errors.add(getMessage("error.subsection.not.bold", uiLanguage, text));
                }

                // Перевірка на відступ
                if (para.getIndentationFirstLine() <= 0) {
                    errors.add(getMessage("error.subsection.not.indented", uiLanguage, text));
                }

                // Перевірка на крапку в кінці
                if (text.endsWith(".")) {
                    errors.add(getMessage("error.subsection.has.period", uiLanguage, text));
                }
            }
        }

        return errors;
    }

    /**
     * Виконати всі перевірки заголовків і повернути об'єднані помилки
     * @param document Документ XWPFDocument для перевірки
     * @param docLanguage Мова документа (UA/EN)
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список всіх знайдених помилок
     */
    public List<String> checkAllHeadings(XWPFDocument document, String docLanguage, String uiLanguage, String officeLocale) {
        List<String> allErrors = new ArrayList<>();

        allErrors.addAll(checkRequiredSections(document, docLanguage, uiLanguage));
        allErrors.addAll(checkHeadingSpacing(document, uiLanguage, officeLocale));
        allErrors.addAll(checkHeadingOrder(document, uiLanguage, officeLocale));
        allErrors.addAll(checkChapterFormatting(document, uiLanguage, officeLocale));
        allErrors.addAll(checkSubsectionFormatting(document, uiLanguage, officeLocale));

        return allErrors;
    }
}
