package csit.semit.semitchecker.errorschecking;

import csit.semit.semitchecker.serviceenums.Lang;
import csit.semit.semitchecker.serviceenums.PerelikType;
import lombok.Getter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class ErrorsPerelikiCheck implements IErrorsCheckable {

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
        //Створюєтся перелік для зберігання помилок
//        ErrorsPerelikiCheck errPerCheck = new ErrorsPerelikiCheck();
//        System.out.println("\nПОМИЛКИ ПЕРЕВІРКИ ПЕРЕЛІКІВ: "+typeErrors);
        ErrorsList errPereliki = new ErrorsList(checkParams.getLocaleWord(), checkParams.getLocaleDoc(), typeErrors);
        //Проверка всіх типів переліків, визначених в enum PerelikType
        for (PerelikType type : PerelikType.values()) {
//            System.out.println("CHECKING PERELIKS ---  " + type+"............");
            errPereliki = this.checkPereliksOfType(xwpfDocument, checkParams, errPereliki, typeErrors, type);
        }
        return errPereliki;
    }

//    //TODO Перевірка "потенційних" переліків
//    public ErrorsList checkBadPereliks(XWPFDocument xwpfDocument, @NotNull CheckParams checkParams, String typeErrors) {
//        ErrorsList errorsList = new ErrorsList(checkParams.localeWord, checkParams.localeDoc, typeErrors);
//        List<XWPFParagraph> xwpfParagraphs = xwpfDocument.getParagraphs();
//
//        //"Правильне" форматування переліків задається 5 стилями
//        //TODO Але деякі студенти форматують або стандартними переліками, або вручну.
//        //Списки, створені "неправильним" чином, не будуть аналізуватися при перевірці.
//        //Тому варто застосувати правильне форматування, що забезпечить аналіз інших фрагментів.
////        for (XWPFParagraph paragraph: xwpfParagraphs) {
////
////        }
//        return errorsList;
//    }

    //Метод перевірки переліків заданого типу
    public ErrorsList checkPereliksOfType(XWPFDocument xwpfDocument, CheckParams checkParams,
                                          ErrorsList errorsList, String typeErrors, PerelikType pt) {
//        System.out.println("\nCHECKING PERELIKS ---  " + pt);

        //Checking all marked lists
        ErrorsPerelikiCheck errPerCheck = new ErrorsPerelikiCheck();
        //Перевірка кількості речень у переліках


        //Детальна перевірка переліку
        int startParagrph = 0;
        ErrorsPerelikiCheck.Perelik p;
        do {
            //Find perelik
            p = errPerCheck.foundPerelik(pt, startParagrph, xwpfDocument, checkParams.getLocaleWord());
            if (p != null) {
                //Перевірка абзаців  // Check perelik
                p = errPerCheck.checkPerelik(p, checkParams, typeErrors);
                //Add mistakes in common list
                errorsList.addErrorList(p.getErrorsList());
                //Початок пошуку наступного переліку встановлюється на наступний абзац після переліку
                //якщо не знайдений кінець тексту
                if (p.getPosStartList() + p.getPerelikItems().size() < xwpfDocument.getParagraphs().size()) {
                    startParagrph = p.getPosStartList() + p.getPerelikItems().size();
                } else {
                    p = null;
                }
            }
        } while (p != null);


        return errorsList;
    }

    //Методи для аналізу всіх переліків:
    //Атрибутом для кожного з них буде
    //1)	Знайти перелік;
    //2)	Знайти речення перед переліком;
    //3)	Знайти непусте речення перед переліком;
    //4)	Знайти речення після переліку;
    //5)	Знайти непустий абзац речення
    //6) перевірити параметри абзацу на відповідність.
    //Може тоді й шукати все відразу! Абзаци, які утворюють перелік,
    // а від першого та останнього пункту переліку шукати вже параграфи до та після


    //1)	Знайти перелік
    //Результат - перелік абзаців, що утворюють перелік
    @Getter
    public static class Perelik {
        //Тип переліку
        private PerelikType perelikType;
        private String perelikPlace;
        private int posStartList;
        private List<XWPFParagraph> perelikItems;
        private XWPFParagraph paragraphBefore;
        private XWPFParagraph paragraphAfter;
        private XWPFParagraph paragraphAfter2;
        private ErrorsList errorsList;
//TODO Для виявлення багаторівневих переліків
//        private int levelNumber;
//        private Map<XWPFParagraph, List<Perelik>> levelDownPereliks;


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Perelik{");
            sb.append(" ").append(perelikType);
            sb.append(": '").append(perelikPlace).append('\'');
            sb.append(", start=").append(posStartList);
            sb.append(", Before=").append(paragraphBefore.getText() != null ? paragraphBefore.getText() : "пустий рядок");
            sb.append('}');
            return sb.toString();
        }
    }

    //Для поеднання декількох фраз параграфу у одне
    private void replaceParagraphText(XWPFParagraph paragraph, String newText) {
        // Удаляем все существующие XWPFRun
        int runCount = paragraph.getRuns().size();
        for (int i = runCount - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        // Создаем новый XWPFRun и устанавливаем текст
        XWPFRun run = paragraph.createRun();
        run.setText(newText);
    }

    //Склейка послідовних параграфів у один. Для аналізу ListNumeric1
    public XWPFParagraph joinParagraphs(List<XWPFParagraph> paragraphs, int startPos, int count) {
        XWPFParagraph joinedParagraph = paragraphs.get(startPos);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            XWPFParagraph p = paragraphs.get(startPos + i);
            for (XWPFRun run : p.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    sb.append(text);
                }
            }
            if (i < count - 1) {
                sb.append(System.lineSeparator()); // Добавляем пробел между абзацами
            }
        }
        replaceParagraphText(joinedParagraph, sb.toString());
        return joinedParagraph;
    }

    //Для розрахунку кількості речень у фразі
    public static int countSentences(String paragraphText) {
        List<String> abbreviations = Arrays.asList("п.", "табл.", "рис.", "pict.", "табл.", "tab.", "с.", "С.", "p.", "P.", "див.");
        String preprocessedText = paragraphText;
        for (String abbr : abbreviations) {
            String safeAbbr = abbr.replace(".", "[dot]");
            preprocessedText = preprocessedText.replace(abbr, safeAbbr);
        }
        preprocessedText = preprocessedText.replaceAll("\\b\\d+(?:\\.\\d+)*\\)", "##NUMBER##");
        preprocessedText = preprocessedText.trim().replaceAll(":$", "."+System.lineSeparator());
        preprocessedText = preprocessedText.trim().replaceAll(";$", "."+System.lineSeparator());
        String[] sentences = preprocessedText.split("(?<=[.])\\s+(?=\\p{Lu}|$)");
//        for (int i = 0; i < sentences.length; i++) {
//            sentences[i] = sentences[i].replace("[dot]", ".");
//        }
        return sentences.length;
    }

//    //Метод для пошуку параграфів Numeric1
//    //В него входит набор параграфов до наступного параграфу типу Numeric1 з ознакою нумерації Продовжити
//    public XWPFParagraph findNumericOneItem(List<XWPFParagraph> xwpfParagraphs, int startPos) {
//        XWPFParagraph numericOneItem = xwpfParagraphs.get(startPos);
//        int countP = 1;
////        do {
////
////        } while
////        for (int i = startPos+1, pNumber = xwpfParagraphs.size(); i < pNumber; i++) {
////            XWPFParagraph paragraph = xwpfParagraphs.get(i);
////            if (paragraph.getStyle() != null && !paragraph.getStyle().equals(PerelikType.ListNumeric1)) {
////                //Якщо знайдене форматування переліку ListNumeric1, то це наступний пункт
////                if (posStartList == -1) {
////                    posStartList = i;
////                }
////                //Якщо не початок - то збільшується кількість рядків переліку
////                listSize++;
////            } else {
////                //Якщо це параграф, після переліку, то  потрібна структура (перелік) сформована
////                if (posStartList != -1) {
////                    break;
////                }
////            }
////        }
//
//        return this.joinParagraphs(xwpfParagraphs, startPos, countP);
//    }

    //Метод, що відшукує перелік типу pt  у документі xwpfDocument, починаючи з заданого абзацу startPos
    //Тобто метод готовий для пошуку переліку довільного типу
    public Perelik foundPerelik(PerelikType pt, int startPos, @NotNull XWPFDocument xwpfDocument, Locale localeWord) {
        List<XWPFParagraph> xwpfParagraphs = xwpfDocument.getParagraphs();
        int posStartList = -1;
        int listSize = 0;
        for (int i = startPos, pNumber = xwpfParagraphs.size(); i < pNumber; i++) {
            XWPFParagraph paragraph = xwpfParagraphs.get(i);
            if (paragraph.getStyle() != null && paragraph.getStyle().equals(pt.name())) {
                //Якщо знайдене форматування переліку і це вперше - це початок переліку
                if (posStartList == -1) {
                    posStartList = i;
                }
                //Якщо не початок - то збільшується кількість рядків переліку
                listSize++;
            } else {
                //Якщо це параграф, після переліку, то  потрібна структура (перелік) сформована
                if (posStartList != -1) {
                    break;
                }
            }
        }
        Perelik resFirst = null;
        //Додати рядок, що передує переліку, в перелік (якщо є)
        if (posStartList > 0) {
//            System.out.println("\nNew marked list");
            if (listSize > 0) {
                resFirst = new Perelik();
                resFirst.perelikType = pt;
                //Додати рядок, що йде перед переліком (якщо є)
                resFirst.paragraphBefore = xwpfParagraphs.get(posStartList - 1);
                //Додати рядок, що йде після переліку (якщо є)
                if (posStartList + listSize < xwpfParagraphs.size()) {
                    resFirst.paragraphAfter = xwpfParagraphs.get(posStartList + listSize);
//                    System.out.println("resFirst.paragraphAfter: "+resFirst.paragraphAfter.getText());
                }
                //Додати рядок, що йде через один після переліку (якщо є)
                if (posStartList + listSize + 1 < xwpfParagraphs.size()) {
                    resFirst.paragraphAfter2 = xwpfParagraphs.get(posStartList + listSize + 1);
//                    System.out.println("resFirst.paragraphAfter2: "+resFirst.paragraphAfter2.getText());
                }
                //Додати рядки переліку
                resFirst.perelikItems = new ArrayList<>();
                for (int i = posStartList, endPos = posStartList + listSize; i < endPos; i++) {
                    resFirst.perelikItems.add(xwpfParagraphs.get(i));
                }
                resFirst.posStartList = posStartList;
                //Знайти місце - пункт, в якому міститься
                resFirst.perelikPlace = findHeader(xwpfParagraphs, posStartList, localeWord);
                String firstItem = resFirst.perelikItems.get(0).getText();
                int lengtFirstItem = firstItem.length() < 100 ? firstItem.length() : 100;
                resFirst.perelikPlace += ": " + pt + "<br>\"... " + firstItem.substring(0, lengtFirstItem) + "\"";
            }
        }

        return resFirst;
    }

    //Метод, що знаходить заголовок, найближчий до абзацу із індексом posStartFind
    //Поки працює для укр та ру локалей Word
    public String findHeader(@NotNull List<XWPFParagraph> xwpfParagraphs, int posStartFind, Locale localWord) {
        //Готуються дані про стилі в залежності від призначеної локації
        //Загрузити локацію та назви стилів заголовків
        ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles.docstyles.docswordstyles", localWord);
        String noheader = bundle.getString("noheader");
        String h1 = bundle.getString("H1");
        String h2 = bundle.getString("H2");
        String h3 = bundle.getString("H3");
        String h4 = bundle.getString("H4");
        //Визначається заголовок частини документу, в якому знайдений перелік
        String place = noheader;
        int i = posStartFind;
        boolean findEnd = false;
        XWPFParagraph p;
        do {
            p = xwpfParagraphs.get(i);
            if (p.getStyle() != null) {
                if (p.getStyle().equals(h4) || p.getStyle().equals(h3)
                        || p.getStyle().equals(h2) || p.getStyle().equals(h1)) {
                    int sizeHeader = p.getText().length() <= 27 ? p.getText().length() : 27;
                    place = p.getText().substring(0, sizeHeader) + "... ";
                    findEnd = true;
                }
            }
            if (!findEnd) {
                i--;
            }
        } while (i >= 0 && !findEnd);
        return place;
    }


    //Метод, що аналізує перелік типу pt у документі xwpfDocument, починаючи з заданого абзацу startPos
    //Тобто метод готовий для пошуку переліку довільного типу
    public Perelik checkPerelik(Perelik perelik, @NotNull CheckParams checkParams, String typeErrors) {
        Perelik perelikWithErrors = perelik;
//        System.out.println(perelik);
        //Підготовка переліку для накопичення помилок
        perelikWithErrors.errorsList = new ErrorsList(checkParams.getLocaleDoc(), checkParams.getLocaleWord(), typeErrors);
        List<XWPFParagraph> listParagraphs = perelikWithErrors.perelikItems;
        //Відсікається з даної перевірки перелік у "Список джерел інформації"
        //Його перевірка буде реалізована окремо.
        //Загрузити локацію та назви стилів заголовків
        ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles.docskeywords.docskeywords", checkParams.getLocaleWord());
        String litra = bundle.getString("litra");
        if (perelik.getPerelikPlace().toUpperCase().startsWith(litra)) {
            //Якщо місце цього переліку в секції із джерелами, то просто вийти із нульовим переліком помилок
            perelik.errorsList = perelikWithErrors.errorsList;
            return perelik;
        }

        //Два переліка не можуть використовуватись в певних текстах
//        ListNumericAua не використовується в инглиш текстах
//        ListNumericAen не використовується в укр текстах
        if ((perelik.perelikType == PerelikType.ListNumericAua) && (checkParams.localeDoc == Lang.EN.getLocale())) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.bad_localAua");
        }
        if ((perelik.perelikType == PerelikType.ListNumericAen) && (checkParams.localeDoc == Lang.UA.getLocale())) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.bad_localAen");
        }


        //Аналіз абзацу, що йде перед переліком
        //Перелік не може бути першим в документі, а також першим після заголовку.
        //В кінці речення перед переліком має стояти потрібний знак: перед складними переліками крапка, перед простими двокрапка
        //Рядок перед переліком не пропускається
        //pereliki.list.atstarttext: перелік не може бути першим у документі
        XWPFParagraph parBefore = perelik.getParagraphBefore();
        if (parBefore == null) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.at_start_text");
        } else {
            //pereliki.list.emptyrowbefore: треба прибрати пустий рядок перед переліком
            if (parBefore.getText().trim().isEmpty()) {
                perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.empty_row_before");
            } else {
                //Перевірити, чи це Нормал стиль
                if (parBefore.getStyle() == null) {
                    //pereliki.list.nonormal_prev_sentence_last_symbol:
                    // неправильний символ у попередньому реченні: '.' треба замінити на ':' (або навпаки для ListNumeric1)
                    if (!parBefore.getText().trim().endsWith(perelikWithErrors.perelikType.getPrevSentSymbol())) {
                        perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(),
                                "pereliki.list.unnormal_prev_sentence_last_symbol");
                    }
                } else {
                    //Якщо заголовок, то треба додати якесь речення-пояснення
                    if (isHeader(parBefore, checkParams.getLocaleWord())) {
                        //pereliki.list.withoutexplanetext: між заголовком та текстом треба вставляти пояснювальну фразу
                        perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.without_explained_text");
                    } else {
                        //може це список іншого типу?
                        //якщо нічого з припустимого, то це якесь незрозуміле форматування, - рекомендується перевірити
                        if (!isAllowedList(parBefore, perelik.perelikType)) {
                            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(),
                                    "pereliki.list.nonormal_prev_sentence_style");
                        }
                    }
                }
            }
        }

        //Перевірка рядків після абзацу
        XWPFParagraph parAfter = perelik.getParagraphAfter();
        XWPFParagraph parAfter2 = perelik.getParagraphAfter();
        //Після переліку та продовженням пункту немає бути вільного рядка
        //pereliki.list.empty_line_after: після переліку немає бути вільного рядка
        if (parAfter != null && parAfter2 != null) {
            if (parAfter.getText().isBlank() && parAfter2.getStyle() == null) {
                perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.empty_line_after");
            }
            //Після абзацу та заголовком БАЖАНО мати деякий текст
            //pereliki.list.empty_line_before_header: WARNING: після переліку перед заголовком бажано представити фразу-підсумок
            if (parAfter.getText().isBlank() && isHeader(parAfter2, checkParams.localeWord)) {
                perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.empty_line_before_header");
            }
        }

//        TODO pereliki.list.onlyonyitem: перелік містить тільки один пункт
//        if (listParagraphs.size() == 1) {
//            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.only_one_item");
//        }

        //pereliki.items.lastitemnonormal: останній пункт переліку закінчується не ‘.’
        //Також треба окремо враховувати складні переліки...
        XWPFParagraph paragraphLast = listParagraphs.get(listParagraphs.size() - 1);
        if (!paragraphLast.getText().trim().endsWith(".")) {
            //В багаторівневих переліках останній символ може бути не ".", якщо далі йде пункт переліку не Numeric1
            if (!((isPerelik(parAfter) != null) && !isPerelik(parAfter).equals(PerelikType.ListNumeric1)
                    && (paragraphLast.getText().trim().endsWith(";") || paragraphLast.getText().trim().endsWith(":")))) {
                //Або в багаторівневому переліку останній символ може бути не ".", якщо далі йде пункт переліку не Numeric1 або Numeric1)
                if (!((isPerelik(parAfter) != null) &&
                        !(isPerelik(parAfter).equals(PerelikType.ListNumeric1) || isPerelik(parAfter).equals(PerelikType.ListNumericWithBracket))
                        && paragraphLast.getText().trim().endsWith(":"))) {

                    perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.last_item_nonormal");
                }
            }
        }

        //pereliki.items.nonormalmiddleitem: в деяких пунктах (крім останнього) в кінці '.' треба замінити на ';' (або навпаки для ListNumeric1)
        boolean badLastChar = false;
        for (int i = 0; i < listParagraphs.size() - 1; i++) {
            badLastChar = badLastChar || (!listParagraphs.get(i).getText().trim().endsWith(perelikWithErrors.perelikType.getLastSymbol()));
        }
        if (badLastChar) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.nonormal_middle_item");
        }

        //pereliki.items.capsstartsymbol: в деяких пунктах на початку треба змінити прописну літеру на рядкову (або навпаки для ListNumeric1)
        boolean badFirstChar = false;
        for (int i = 0; i < listParagraphs.size(); i++) {
            badFirstChar = badFirstChar || (!listParagraphs.get(i).getText().trim().substring(0, 1).matches(perelikWithErrors.perelikType.getMaskFirstSymbol()));
        }
        if (badFirstChar) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.caps_start_symbol");
        }

        //pereliki.items.wrong_semicolon_caps: в деяких пунктах зустрічається текст з великої літери після двокрапки
        boolean badSemicolonCaps = false;
        Pattern pattern = Pattern.compile(".*:( |\u00A0)\\p{Lu}.*");
        for (int i = 0; i < listParagraphs.size(); i++) {
            badSemicolonCaps = badSemicolonCaps || (pattern.matcher(listParagraphs.get(i).getText().trim()).matches());
        }
        if (badSemicolonCaps) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.wrong_semicolon_caps");
        }

//        pereliki.items.bad_count_numericone=пункти переліку ListNumeric1 складаються з двох та більше речень
//        Для отримання такої помилки треба, щоб всі пункти складалися з одного абзацу. Якщо хоча б один містить 2 речення та більш, то
//        застосування переліку ListNumeric1 обгрунтоване. Якщо виявлена ситуація, описана наступною, то це виключає цю помилку
//        TODO - треба аналізувати ID переліків. Але потрібно підсилювати логіку
//        pereliki.items.bad_uncorrect_numericone=деякий пункт переліку ListNumeric1 містить одне речення, \
//        <br>але за ним йде звичайний текст. Розгляньте можливість поєднати їх в один абзац
//        pereliki.items.bad_countitem=пункти такого переліку складаються тільки з одного речення
//        Якщо хоч один абзац містить два речення, то слід видавати таку помилку
        boolean badCountNumericOne = true;
//        boolean badCountNumericOneButJoin = false;
        boolean badCountListItem = true;
        for (int i=0; i<perelik.perelikItems.size();i++) {
            XWPFParagraph par = perelik.perelikItems.get(i);
            int countS;
            if (par.getText() != null && par.getStyle() != null) {
                if (par.getStyle().equals(PerelikType.ListNumeric1.name())) {
                    countS = ErrorsPerelikiCheck.countSentences(par.getText());
                    if (countS == 1) {
                            badCountNumericOne = false;
                    }
                } else {
                    // iнші типи переліків - по одному
                    countS = ErrorsPerelikiCheck.countSentences(par.getText());
                    if (countS > 1) {
                        badCountListItem = false;
                    }
                }
            }
        }
        if (!badCountNumericOne) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.bad_count_numericone");
        }
        if (!badCountListItem) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.bad_countitem");
        }
        return perelikWithErrors;
    }

    private boolean isAllowedList(XWPFParagraph parBefore, PerelikType perelikType) {
        //На нуль не перевіряєм - бо в метод вхіде буде після перевірку на дефолтний абзац
        PerelikType ptBefore = PerelikType.getPerelikTypeByStyleName(parBefore.getStyle());
        boolean res = true;
        if (ptBefore == null) {
            res = false;
        } else {
            //це перелік
            if (ptBefore.equals(perelikType)) {
                res = false;
            }
        }
        return res;
    }

    private PerelikType isPerelik(@NotNull XWPFParagraph paragraph) {
        if (paragraph.getStyle() == null) return null;
        return PerelikType.getPerelikTypeByStyleName(paragraph.getStyle());
    }

    private boolean isHeader(XWPFParagraph p, Locale localWord) {
        if (p == null) {
            return false;
        } else {
            if (p.getStyle() != null) {
                //Готуються дані про стилі в залежності від призначеної локації
                //Загрузити локацію та назви стилів заголовків
                ResourceBundle bundle = ResourceBundle.getBundle("resourcesbundles.docstyles.docswordstyles", localWord);
                String h1 = bundle.getString("H1");
                String h2 = bundle.getString("H2");
                String h3 = bundle.getString("H3");
                String h4 = bundle.getString("H4");
                if (p.getStyle().equals(h4) || p.getStyle().equals(h3)
                        || p.getStyle().equals(h2) || p.getStyle().equals(h1)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }


}
