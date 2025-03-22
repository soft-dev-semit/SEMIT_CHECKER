package csit.semit.semitchecker.docutils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocStatistic {
    //імя файлу
    private String filename;
    //мова тексту
    private String docLocale;
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
    private String abstractRow;


    public void prepareAbstract() {
        StringBuilder sb = null;
        //English options
        if (docLocale.equals("UA")) {
            sb = new StringBuilder("Реферат: ");
            if (countPages != 0) {
                sb.append(countPages).append(" стор.");
                if (countFigures != 0) {
                    sb.append(", ").append(countFigures).append(" рис.");
                }
                if (countTables != 0) {
                    sb.append(", ").append(countTables).append(" табл.");
                }
                if (countSources != 0) {
                    sb.append(", ").append(countSources).append(" джерел");
                }
                if (countAppendixes != 0) {
                    sb.append(", ").append(countAppendixes).append(" додатків");
                }
            } else {
                sb.append("помилки обробки файлу");
            }
        } else {
//            docLocale.equals("EN")
            sb = new StringBuilder("Abstract: ");
            if (countPages == 0) {
                sb.append("wrong file processing");
            }
        }
        abstractRow = sb.toString();
    }
}
