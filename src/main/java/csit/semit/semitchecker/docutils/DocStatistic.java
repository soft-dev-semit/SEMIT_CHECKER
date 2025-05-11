package csit.semit.semitchecker.docutils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocStatistic {
    //імя файлу
    private String filename;
    //мова Ворд
    private Locale wordLocale;
    //мова тексту
    private Locale docLocale;
    //Границі
    private double leftMargin;
    private double rightMargin;
    private double topMargin;
    private double bottomMargin;
    //Склад
    private int countPages;
    private int countFigures;
    private int countTables;
    private int countSources;
    private int countAppendixes;
    //Рядок для реферату (заповнюється програмно)
    private String abstractUARow;
    private String abstractENRow;


    public void prepareAbstractUA() {
        StringBuilder sb = null;
        //Ukrainian options
        sb = new StringBuilder("Реферат: ");
        if (countPages > 0) {
            String countPagesStr = countPages==777? "NN" : ""+countPages;
            sb.append(countPagesStr).append(" стор.");
            if (countFigures > 0) {
                sb.append(", ").append(countFigures).append(" рис.");
            }
            if (countTables > 0) {
                sb.append(", ").append(countTables).append(" табл.");
            }
            if (countSources > 0) {
                sb.append(", ").append(countSources).append(" джерел");
            }
            if (countAppendixes > 0) {
                sb.append(", ").append(countAppendixes).append(" додатків");
            }
        } else {
            sb.append("помилки обробки файлу");
        }
        abstractUARow = sb.toString();
    }

    public void prepareAbstractEN() {
        StringBuilder sb = null;
        //English options
        sb = new StringBuilder("Abstract: ");
        if (countPages > 0) {
            String countPagesStr = countPages==777? "NN" : ""+countPages;
            sb.append(countPagesStr).append(" p.");
            if (countFigures > 0) {
                sb.append(", ").append(countFigures).append(" fig.");
            }
            if (countTables > 0) {
                sb.append(", ").append(countTables).append(" tabl.");
            }
            if (countSources > 0) {
                sb.append(", ").append(countSources).append(" soureces");
            }
            if (countAppendixes > 0) {
                sb.append(", ").append(countAppendixes).append(" appendexes");
            }
        } else {
            sb.append("помилки обробки файлу");
        }
        abstractENRow = sb.toString();
    }


}
