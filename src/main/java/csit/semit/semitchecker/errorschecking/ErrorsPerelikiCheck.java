package csit.semit.semitchecker.errorschecking;

import csit.semit.semitchecker.serviceenums.PerelikType;
import lombok.Getter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class ErrorsPerelikiCheck implements IErrorsCheckable {

    @Override
    public ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors) {
//        System.out.println("CHECKING......  " + typeErrors);

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

    //Метод перевірки переліків
    public ErrorsList checkPereliksOfType(XWPFDocument xwpfDocument, CheckParams checkParams,
                                          ErrorsList errorsList, String typeErrors, PerelikType pt) {
//        System.out.println("\nCHECKING PERELIKS ---  " + pt);

        //Checking all marked lists
        ErrorsPerelikiCheck errPerCheck = new ErrorsPerelikiCheck();
        int startParagrph = 0;
        ErrorsPerelikiCheck.Perelik p = null;
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
    public class Perelik {
        //Тип переліку
        private PerelikType perelikType;
        private String perelikPlace;
        private int posStartList;
        private List<XWPFParagraph> perelikItems;
        private XWPFParagraph paragraphBefore;
        private XWPFParagraph paragraphAfter;
        private XWPFParagraph paragraphAfter2;
        private ErrorsList errorsList;

    }

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
                if ((posStartList - 1) > -1) {
                    resFirst.paragraphBefore = xwpfParagraphs.get(posStartList - 1);
//                    System.out.println("resFirst.paragraphBefore: " + resFirst.paragraphBefore.getText());
                }
                //Додати рядок, що йде після переліку (якщо є)
                if (posStartList + listSize <= xwpfParagraphs.size()) {
                    resFirst.paragraphAfter = xwpfParagraphs.get(posStartList + listSize);
//                    System.out.println("resFirst.paragraphAfter: "+resFirst.paragraphAfter.getText());
                }
                //Додати рядок, що йде через один після переліку (якщо є)
                if (startPos + listSize + 1 <= xwpfParagraphs.size()) {
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
                int lengtFirstItem = firstItem.length() < 30 ? firstItem.length() : 30;
                resFirst.perelikPlace += ": \"... " + firstItem.substring(0, lengtFirstItem) + "\"";
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
        //TODO Для цих помилок у переліках було б доречно показувати, який тип переліку аналізується
        perelikWithErrors.errorsList = new ErrorsList(checkParams.getLocaleDoc(), checkParams.getLocaleWord(), typeErrors);
        List<XWPFParagraph> listParagraphs = perelikWithErrors.perelikItems;
        XWPFParagraph parFirst = listParagraphs.get(0);
        String errorMsgText = "";

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
            if (parBefore.getText().isEmpty()) {
                perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.empty_row_before");
            } else {
                //Перевірити, чи це Нормал стиль
                if (parBefore.getStyle() == null) {
                    //pereliki.list.nonormal_prev_sentence_last_symbol:
                    // неправильний символ у попередньому реченні: '.' треба замінити на ':' (або навпаки для ListNumeric1)
                    if (!parBefore.getText().endsWith(perelikWithErrors.perelikType.getPrevSentSymbol())) {
                        perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(),
                                "pereliki.list.nonormal_prev_sentence_last_symbol");
                    }
                } else {
                    //Якщо заголовок, то треба додати якесь речення-пояснення
                    if (isHeader(parBefore, checkParams.getLocaleWord())) {
                        //pereliki.list.withoutexplanetext: між заголовком та текстом треба вставляти пояснювальну фразу
                        perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.without_explained_text");
                    } else {
                        //якщо не заголовок, то це якесь незрозуміле форматування, - рекомендується перевірити
                        perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(),
                                "pereliki.list.nonormal_prev_sentence");
                    }
                }
            }
        }

        //TODO виявити "типу списки" - звичайні абзаци, в яких на початку йдуть послідовності символів, які починають списки


        //TODO Аналіз абзаців після тексту
        XWPFParagraph parAfter = perelik.getParagraphAfter();
        XWPFParagraph parAfter2 = perelik.getParagraphAfter();


        //pereliki.list.onlyonyitem: перелік містить тільки один пункт
        if (listParagraphs.size() == 1) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.list.only_one_item");
        }


        //pereliki.items.lastitemnonormal: останній пункт переліку закінчується не ‘.’
        //TODO В багаторівневих переліках останній символ може бути не "."!
        //Тоді треба окремо аналізувати складні переліки...
        XWPFParagraph paragraphLast = listParagraphs.get(listParagraphs.size() - 1);
        if (!paragraphLast.getText().endsWith(".")) {

            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.last_item_nonormal");
        }

        //pereliki.items.nonormalmiddleitem: в деяких пунктах (крім останнього) в кінці '.' треба замінити на ';' (або навпаки для ListNumeric1)
        boolean badLastChar = false;
        for (int i = 0; i < listParagraphs.size() - 1; i++) {
            badLastChar = badLastChar || (!listParagraphs.get(i).getText().endsWith(perelikWithErrors.perelikType.getLastSymbol()));
        }
        if (badLastChar) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.nonormal_middle_item");
        }

        //pereliki.items.capsstartsymbol: в деяких пунктах на початку треба змінити прописну літеру на рядкову (або навпаки для ListNumeric1)
        boolean badFirstChar = false;
        for (int i = 0; i < listParagraphs.size(); i++) {
            badFirstChar = badFirstChar || (!listParagraphs.get(i).getText().matches(perelikWithErrors.perelikType.getMaskFirstSymbol()));
        }
        if (badFirstChar) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.caps_start_symbol");
        }

        //pereliki.items.wrong_semicolon_caps: в деяких пунктах зустрічається текст з великої літери після двокрапки
        boolean badSemicolonCaps = false;
        for (int i = 0; i < listParagraphs.size(); i++) {
            badSemicolonCaps = badSemicolonCaps || (!listParagraphs.get(i).getText().matches(": [A-ZА-ЯІЇЄ]"));
        }
        if (badSemicolonCaps) {
            perelikWithErrors.errorsList.addError(perelikWithErrors.getPerelikPlace(), "pereliki.items.wrong_semicolon_caps");
        }

        return perelikWithErrors;
    }

    private boolean isHeader(XWPFParagraph p, Locale localWord) {
        if (p == null) {
            return false;
        } else {
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
        }
    }


}
