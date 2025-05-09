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
    private final TypicalHeadingStyles typicalHeadingStyles;

    @Autowired
    public CheckHeadings(MessageSource messageSource, TypicalHeadings typicalHeadings, TypicalHeadingStyles typicalHeadingStyles) {
        this.messageSource = messageSource;
        this.typicalHeadings = typicalHeadings;
        this.typicalHeadingStyles = typicalHeadingStyles;
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
     * Перевірити, чи є абзац типовим заголовком без стилю заголовка
     * @param para Абзац для перевірки
     * @param docLanguage Мова документа (UA/EN)
     * @param headingStyles Список стилів заголовків для виключення
     * @return true, якщо абзац є типовим заголовком без стилю
     */
    private boolean isUnstyledTypicalHeading(XWPFParagraph para, String docLanguage, List<String> headingStyles) {
        String text = para.getText().trim();
        if (text.isEmpty()) return false;
        if (para.getStyle() != null && headingStyles.contains(para.getStyle())) return false;
        return typicalHeadings.isStandardHeading(text, docLanguage) && !text.matches(".*[.,!?;]$");
    }

    /**
     * Перевірити, чи є абзац нумерованим заголовком без стилю заголовка
     * @param para Абзац для перевірки
     * @param headingStyles Список стилів заголовків для виключення
     * @return true, якщо абзац є нумерованим заголовком без стилю
     */
    private boolean isUnstyledNumberedHeading(XWPFParagraph para, List<String> headingStyles) {
        String text = para.getText().trim();
        if (text.isEmpty()) return false;
        if (para.getStyle() != null && headingStyles.contains(para.getStyle())) return false;
        // Patterns: "1 Chapter", "1.1 Chapter", "1.1.1 Chapter", "1.1.1.1 Chapter"
        // or "1 Chapter. Chapter", "1.1 Chapter. Chapter", etc.
        return (text.matches("^\\d+(\\.\\d+){0,3}\\s+\\w+.*$") ||
                text.matches("^\\d+(\\.\\d+){0,3}\\s+\\w+.*\\.\\s+\\w+.*$")) &&
                !text.matches(".*[.,!?;]$");
    }

    /**
     * Перевірити, чи є абзац типовим заголовком без стилю (для тестування)
     * @param para Абзац для перевірки
     * @param docLanguage Мова документа (UA/EN)
     * @param headingStyles Список стилів заголовків для виключення
     * @return true, якщо абзац є типовим заголовком без стилю
     */
    public boolean isUnstyledTypicalHeadingForTest(XWPFParagraph para, String docLanguage, List<String> headingStyles) {
        return isUnstyledTypicalHeading(para, docLanguage, headingStyles);
    }

    /**
     * Перевірити, чи є абзац нумерованим заголовком без стилю (для тестування)
     * @param para Абзац для перевірки
     * @param headingStyles Список стилів заголовків для виключення
     * @return true, якщо абзац є нумерованим заголовком без стилю
     */
    public boolean isUnstyledNumberedHeadingForTest(XWPFParagraph para, List<String> headingStyles) {
        return isUnstyledNumberedHeading(para, headingStyles);
    }

    /**
     * Отримати список обов'язкових розділів для заданої мови
     * @param docLanguage Мова документа (UA/EN)
     * @return Масив обов'язкових розділів
     */
    public String[] getRequiredSections(String docLanguage) {
        return typicalHeadings.getAllHeadings(docLanguage);
    }

    /**
     * Витягти всі заголовки (стилізовані та нестилізовані) з документа
     * @param document Документ XWPFDocument
     * @param docLanguage Мова документа (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список заголовків із рівнями (1, 2, 3 для стилізованих, 0 для нестилізованих)
     */
    public List<Map.Entry<String, Integer>> extractAllHeadings(XWPFDocument document, String docLanguage, String officeLocale) {
        List<Map.Entry<String, Integer>> headings = new ArrayList<>();
        List<String> headingStyles = Arrays.asList(
                typicalHeadingStyles.getHeadingStyle(TypicalHeadingStyles.StyleKey.HEADING_1, officeLocale),
                typicalHeadingStyles.getHeadingStyle(TypicalHeadingStyles.StyleKey.HEADING_2, officeLocale),
                typicalHeadingStyles.getHeadingStyle(TypicalHeadingStyles.StyleKey.HEADING_3, officeLocale)
        );

        for (XWPFParagraph para : document.getParagraphs()) {
            String style = para.getStyle();
            String text = para.getText().trim();

            if (text.isEmpty()) continue;

            // Check for styled headings
            if (style != null) {
                if (style.equals(headingStyles.get(0))) { // Heading 1
                    headings.add(new AbstractMap.SimpleEntry<>(text, 1));
                } else if (style.equals(headingStyles.get(1))) { // Heading 2
                    headings.add(new AbstractMap.SimpleEntry<>(text, 2));
                } else if (style.equals(headingStyles.get(2))) { // Heading 3
                    headings.add(new AbstractMap.SimpleEntry<>(text, 3));
                }
            }

            // Check for unstyled headings
            if (isUnstyledTypicalHeading(para, docLanguage, headingStyles) ||
                    isUnstyledNumberedHeading(para, headingStyles)) {
                headings.add(new AbstractMap.SimpleEntry<>(text, 0)); // Level 0 for unstyled headings
            }
        }

        return headings;
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

        List<Map.Entry<String, Integer>> documentHeadings = extractAllHeadings(document, docLanguage, docLanguage);
        List<String> headingTexts = new ArrayList<>();
        for (Map.Entry<String, Integer> heading : documentHeadings) {
            if (heading.getValue() == 1 || heading.getValue() == 0) {
                headingTexts.add(heading.getKey());
            }
        }

        for (String requiredSection : requiredSections) {
            boolean found = false;
            for (String heading : headingTexts) {
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
        List<Map.Entry<String, Integer>> headings = extractAllHeadings(document, officeLocale, officeLocale);

        for (Map.Entry<String, Integer> headingEntry : headings) {
            String headingText = headingEntry.getKey();
            int headingLevel = headingEntry.getValue();

            // Find the paragraph index for this heading
            int paraIndex = -1;
            for (int i = 0; i < paragraphs.size(); i++) {
                if (paragraphs.get(i).getText().trim().equals(headingText)) {
                    paraIndex = i;
                    break;
                }
            }

            if (paraIndex == -1) continue; // Heading not found (unlikely, but for safety)

            // Check for empty line before
            if (paraIndex > 0) {
                XWPFParagraph prevPara = paragraphs.get(paraIndex - 1);
                if (!prevPara.getText().trim().isEmpty()) {
                    errors.add(getMessage("error.no.empty.line.before", uiLanguage, headingText));
                }
            }

            // Check for empty line after
            if (paraIndex < paragraphs.size() - 1) {
                XWPFParagraph nextPara = paragraphs.get(paraIndex + 1);
                if (!nextPara.getText().trim().isEmpty()) {
                    errors.add(getMessage("error.no.empty.line.after", uiLanguage, headingText));
                }
            }

            // Check for sufficient text after Heading 1 or unstyled heading
            if ((headingLevel == 1 || headingLevel == 0) && paraIndex < paragraphs.size() - 2) {
                boolean hasMoreThanOneLine = false;
                for (int j = paraIndex + 1; j < paraIndex + 3 && j < paragraphs.size(); j++) {
                    if (!paragraphs.get(j).getText().trim().isEmpty()) {
                        hasMoreThanOneLine = true;
                        break;
                    }
                }
                if (!hasMoreThanOneLine) {
                    errors.add(getMessage("error.not.enough.text.after.heading", uiLanguage, headingText));
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

        List<Map.Entry<String, Integer>> headings = extractAllHeadings(document, officeLocale, officeLocale);

        for (Map.Entry<String, Integer> headingEntry : headings) {
            String text = headingEntry.getKey();
            int level = headingEntry.getValue();

            // Skip unstyled headings for order check (they will be caught by checkUnstyledHeadings)
            if (level == 0) continue;

            // Skip headings that are all uppercase and don't start with a number
            if (text.equals(text.toUpperCase()) && !text.matches("^\\d+.*")) {
                continue;
            }

            if (level == 1) { // Heading 1
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
            } else if (level == 2) { // Heading 2
                if (text.matches("^\\d+\\.\\d+\\s+.*")) {
                    String[] parts = text.split("\\s+")[0].split("\\.");
                    try {
                        int chapterNum = Integer.parseInt(parts[0]);
                        int sectionNum = Integer.parseInt(parts[1]);

                        if (chapterNum != currentChapter || sectionNum != currentSection + 1) {
                            errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                        }

                        currentSection = sectionNum;
                        currentSubsection = 0;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        errors.add(getMessage("error.wrong.heading.order", uiLanguage, text));
                    }
                }
            } else if (level == 3) { // Heading 3
                if (text.matches("^\\d+\\.\\d+\\.\\d+\\s+.*")) {
                    String[] parts = text.split("\\s+")[0].split("\\.");
                    try {
                        int chapterNum = Integer.parseInt(parts[0]);
                        int sectionNum = Integer.parseInt(parts[1]);
                        int subsectionNum = Integer.parseInt(parts[2]);

                        if (chapterNum != currentChapter || sectionNum != currentSection || subsectionNum != currentSubsection + 1) {
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
        List<Map.Entry<String, Integer>> headings = extractAllHeadings(document, officeLocale, officeLocale);

        for (Map.Entry<String, Integer> headingEntry : headings) {
            String text = headingEntry.getKey();
            int level = headingEntry.getValue();

            // Only check Heading 1 (styled headings)
            if (level != 1) continue;

            // Find the corresponding paragraph to check formatting
            XWPFParagraph para = null;
            for (XWPFParagraph p : document.getParagraphs()) {
                if (p.getText().trim().equals(text)) {
                    para = p;
                    break;
                }
            }

            if (para == null) continue;

            if (!text.equals(text.toUpperCase())) {
                errors.add(getMessage("error.chapter.not.uppercase", uiLanguage, text));
            }

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

            if (para.getAlignment() != ParagraphAlignment.CENTER &&
                    para.getAlignment() != ParagraphAlignment.LEFT &&
                    para.getIndentationFirstLine() == 0) {
                errors.add(getMessage("error.chapter.wrong.alignment", uiLanguage, text));
            }

            if (text.endsWith(".")) {
                errors.add(getMessage("error.chapter.has.period", uiLanguage, text));
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
        List<Map.Entry<String, Integer>> headings = extractAllHeadings(document, officeLocale, officeLocale);

        for (Map.Entry<String, Integer> headingEntry : headings) {
            String text = headingEntry.getKey();
            int level = headingEntry.getValue();

            // Only check Heading 2 and Heading 3 (styled headings)
            if (level != 2 && level != 3) continue;

            // Find the corresponding paragraph to check formatting
            XWPFParagraph para = null;
            for (XWPFParagraph p : document.getParagraphs()) {
                if (p.getText().trim().equals(text)) {
                    para = p;
                    break;
                }
            }

            if (para == null) continue;

            String[] words = text.split("\\s+");

            int startWordIndex = 0;
            if (words.length > 0 && words[0].matches("\\d+(\\.\\d+)*")) {
                startWordIndex = 1;
            }

            if (startWordIndex < words.length) {
                String firstWord = words[startWordIndex];
                if (firstWord.length() > 0) {
                    char firstChar = firstWord.charAt(0);
                    if (!Character.isUpperCase(firstChar)) {
                        errors.add(getMessage("error.subsection.first.not.uppercase", uiLanguage, text));
                    }

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

            if (para.getIndentationFirstLine() <= 0) {
                errors.add(getMessage("error.subsection.not.indented", uiLanguage, text));
            }

            if (text.endsWith(".")) {
                errors.add(getMessage("error.subsection.has.period", uiLanguage, text));
            }
        }

        return errors;
    }

    /**
     * Перевірка абзаців, які виглядають як заголовки, але не мають стилів Заголовок 1, Заголовок 2, Заголовок 3
     * @param document Документ XWPFDocument для перевірки
     * @param docLanguage Мова документа (UA/EN)
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список знайдених помилок
     */
    public List<String> checkUnstyledHeadings(XWPFDocument document, String docLanguage, String uiLanguage, String officeLocale) {
        List<String> errors = new ArrayList<>();
        List<String> headingStyles = Arrays.asList(
                typicalHeadingStyles.getHeadingStyle(TypicalHeadingStyles.StyleKey.HEADING_1, officeLocale),
                typicalHeadingStyles.getHeadingStyle(TypicalHeadingStyles.StyleKey.HEADING_2, officeLocale),
                typicalHeadingStyles.getHeadingStyle(TypicalHeadingStyles.StyleKey.HEADING_3, officeLocale)
        );

        for (XWPFParagraph para : document.getParagraphs()) {
            if (isUnstyledTypicalHeading(para, docLanguage, headingStyles) ||
                    isUnstyledNumberedHeading(para, headingStyles)) {
                errors.add(getMessage("error.unstyled.heading", uiLanguage, para.getText().trim()));
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
        //allErrors.addAll(checkUnstyledHeadings(document, docLanguage, uiLanguage, officeLocale));
        return allErrors;
    }
}