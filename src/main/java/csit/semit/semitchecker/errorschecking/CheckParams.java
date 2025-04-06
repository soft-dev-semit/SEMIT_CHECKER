package csit.semit.semitchecker.errorschecking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

/**
 * Клас для збереження параметрів
 * ПОЛЯ
 * Locale localeInterfaces - мова інтерфейсу; задається локалью, може бути EN (english) та UK (українська).
 *  *             буде обриратися на початку роботи с сервісом;
 * Locale localeDoc - мова документа; задається локалью, може бути EN (english) та UK (українська).
 *             буде задаватися при завантаженні файлу.
 *             Визначає, які ключові слова шукати, наприклад, Таблиця чи Table, ВСТУП чи INTRODUCTION
 * Locale localeWord - локаль MS Office, в якій був збережений документ;
 *             !!!      може бути EN (english), UK (українська), RU (російська). !!!
 *             !!!       Потенційно - будь-яка, потрібне додаткове дослідження   !!!
 *              буде задаватися при завантаженні файлу. ??? Чи може бути визначена автоматично??? === МОЖЕ
 *              А може бути англійська та інші?
 *             Визначає, які СТИЛІ ДОКУМЕНТА шукати, наприклад, 1 чи heading1, 11 чи TOC1
 */
@AllArgsConstructor
@Setter
@Getter
public class CheckParams {

    //Мова відображення інтерфейсу - тільки 2 варіанти, по замовченню UA
    public Locale localeInterfaces = Lang.UA.getLocale();
    //Мова документу - тільки 2 варіанти, по замовченню UA
    public Locale localeDoc;
    //Locale Word - різні варіанти, визначається налаштуваннями ворд при збереженні документу, по замовченню ENGLISH
    public Locale localeWord;

    public CheckParams() {
        localeInterfaces = Lang.UA.getLocale();
        localeDoc = Lang.UA.getLocale();
        localeWord = MultiLang.EN.getLocale();
    }
}
