package csit.semit.semitchecker.errorschecking;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

public interface IErrorsCheckable {

    /**
     * Інтерфейс для визначення класів, що реалізують перевірки за певним критерієм
     *
     * @param xwpfDocument - завантажений документ для перевірки (використовується Apache POI
     * @param typeErrors   - назва типу помилок, використовується для визначення ResourceBundle,
     *                     з якою треба працювати.
     *                     <p>
     *                     Вибір локалі буде залежати від localeDoc та localeWord
     *                     Пропонується:
     *                     -  змінні для пошуку ключових слів документу представити у ResourceBundle typeErrors+"-dockeywords."+localDoc
     *                     -  змінні для пошуку стилів представити у ResourceBundle typeErrors+"-docstyles."+localWord
     *                     -  змінні для повідомлень про помилки представити у ResourceBundle typeErrors+"-errortexts."+localDoc
     *                     Відповідні ResourceBundle слід створювати у підкаталогах
     *                     /resources/resourcesbundles/dockeywords
     *                     /resources/resourcesbundles/docstyles
     *                     /resources/resourcesbundles/errorstexts
     *                     Тобто створюючи клас, що імплементує даний інтерфейс, також треба створювати три ResourceBundle
     *                     у відповідних каталогах (по одному в кожному)
     *                     IMPORTANCE Повідомлення про помилки формувати, використовуючи виключно змінні. Інтерпретація буде виконуватись
     *                     безпосередньо на веб-сторінках
     *                     ДЛЯ ОБГОВОРЕННЯ
     *                     ??? А як все ж таки правильно створити локали для файлів?
     *                     ??? може дефолтну для UA та додаткову для инглиш.
     *                     ??? А для стилів навпаки - дефолтна для инглищ, а інші - ДЛЯ ВСІХ ІНШИХ? Бо є відчуття, що
     *                     стиль Заголовок 1 для РУ і ЮА буде відображатися однаково - "1", як і для кітайської та інших
     *                     Але треба дослідити!
     */
    ErrorsList check(XWPFDocument xwpfDocument, CheckParams checkParams, String typeErrors);

}
