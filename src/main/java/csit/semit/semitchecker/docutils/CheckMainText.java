package csit.semit.semitchecker.docutils;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CheckMainText {
    private XWPFDocument doc;
    private final CheckHeadings checkHeadings;
    private final MessageSource messageSource;

    @Autowired
    public CheckMainText(CheckHeadings checkHeadings, MessageSource messageSource) {
        this.checkHeadings = checkHeadings;
        this.messageSource = messageSource;
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
     * Отримати всі заголовки (стилізовані та нестилізовані) з документа
     * @param doc Документ для аналізу
     * @param docLanguage Мова документа (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список заголовків із рівнями
     */
    public List<Map.Entry<String, Integer>> getAllHeadings(XWPFDocument doc, String docLanguage, String officeLocale) {
        return checkHeadings.extractAllHeadings(doc, docLanguage, officeLocale);
    }

    /**
     * Перевірити налаштування шрифту основного тексту
     * @param doc Документ для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список помилок
     */
    public List<String> checkFontSettings(XWPFDocument doc, String uiLanguage, String officeLocale) {
        this.doc = doc;
        List<String> errors = new ArrayList<>();

        // Get all headings to exclude them
        List<Map.Entry<String, Integer>> headings = getAllHeadings(doc, officeLocale, officeLocale);
        Set<String> headingTexts = headings.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (XWPFParagraph para : doc.getParagraphs()) {
            String text = para.getText().trim();
            if (text.isEmpty() || headingTexts.contains(text)) {
                continue; // Skip empty paragraphs and headings
            }

            for (XWPFRun run : para.getRuns()) {
                // Check font name (should be Times New Roman)
                String fontName = run.getFontName();
                if (fontName == null || !fontName.equals("Times New Roman")) {
                    errors.add(getMessage("error.font.wrong.name", uiLanguage, text, fontName != null ? fontName : "unknown"));
                }

                // Check font size (should be 14)
                int fontSize = run.getFontSize();
                if (fontSize != 14 && fontSize != -1) { // -1 means default, we'll assume it's incorrect
                    errors.add(getMessage("error.font.wrong.size", uiLanguage, text, fontSize));
                }

                // Check font color (should be black, RGB: 0,0,0)
                String color = run.getColor();
                if (color != null && !color.equalsIgnoreCase("000000")) {
                    errors.add(getMessage("error.font.wrong.color", uiLanguage, text, color));
                }
            }
        }

        return errors;
    }

    /**
     * Перевірити налаштування абзаців основного тексту
     * @param doc Документ для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список помилок
     */
    public List<String> checkParagraphSettings(XWPFDocument doc, String uiLanguage, String officeLocale) {
        this.doc = doc;
        List<String> errors = new ArrayList<>();

        // Get all headings to exclude them
        List<Map.Entry<String, Integer>> headings = getAllHeadings(doc, officeLocale, officeLocale);
        Set<String> headingTexts = headings.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (XWPFParagraph para : doc.getParagraphs()) {
            String text = para.getText().trim();
            if (text.isEmpty() || headingTexts.contains(text)) {
                continue; // Skip empty paragraphs and headings
            }

            // Check first line indent (should be 1.25 cm = 709 twips)
            int firstLineIndent = para.getIndentationFirstLine();
            if (firstLineIndent != 709) { // 1.25 cm = 709 twips (1 cm = 567 twips)
                errors.add(getMessage("error.paragraph.wrong.indent", uiLanguage, text, firstLineIndent));
            }

            // Check alignment (should be justified)
            if (para.getAlignment() != ParagraphAlignment.BOTH) {
                errors.add(getMessage("error.paragraph.wrong.alignment", uiLanguage, text, para.getAlignment().toString()));
            }

            // Check line spacing (should be 1.5 = 360 twips for "multiple" rule)
            LineSpacingRule spacingRule = para.getSpacingLineRule();
            int lineSpacing = (int) para.getSpacingBetween();
            if (spacingRule != LineSpacingRule.AUTO || lineSpacing != 360) { // 1.5 * 240 = 360 twips
                errors.add(getMessage("error.paragraph.wrong.line.spacing", uiLanguage, text, lineSpacing));
            }
        }

        return errors;
    }

    /**
     * Виконати всі перевірки основного тексту
     * @param doc Документ для перевірки
     * @param uiLanguage Мова інтерфейсу для повідомлень (UA/EN)
     * @param officeLocale Локаль Office (UA/EN/RU)
     * @return Список всіх помилок
     */
    public List<String> performAllChecks(XWPFDocument doc, String uiLanguage, String officeLocale) {
        this.doc = doc;
        List<String> allErrors = new ArrayList<>();
        allErrors.addAll(checkFontSettings(doc, uiLanguage, officeLocale));
        allErrors.addAll(checkParagraphSettings(doc, uiLanguage, officeLocale));
        return allErrors;
    }
}