package csit.semit.semitchecker.serviceenums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WordStyles {
    NORMAL("noheader","Times New Roman", "black", 14, Boolean.FALSE, Boolean.FALSE,
            "NONE", "BOTH", Integer.valueOf(125), Integer.valueOf(15), Integer.valueOf(0),
            Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)),
    HEADING_1("H1", "Times New Roman", "black", 14, Boolean.TRUE, Boolean.FALSE,
            "NONE", "CENTER", Integer.valueOf(125), Integer.valueOf(15), Integer.valueOf(0),
            Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(21)),
    HEADING_2("H2", "Times New Roman", "black", 14, Boolean.TRUE, Boolean.FALSE,
            "NONE", "BOTH", Integer.valueOf(125), Integer.valueOf(15), Integer.valueOf(0),
            Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)),
    HEADING_3("H3", "Times New Roman", "black", 14, Boolean.TRUE, Boolean.FALSE,
            "NONE", "BOTH", Integer.valueOf(125), Integer.valueOf(15), Integer.valueOf(0),
            Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)),
    HEADING_4("H4", "Times New Roman", "black", 14, Boolean.TRUE, Boolean.FALSE,
            "NONE", "BOTH", Integer.valueOf(125), Integer.valueOf(15), Integer.valueOf(0),
            Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));

    private String styleName;
    private String fontName;
    private String fontColor;
    private Integer fontSize;
    private Boolean isBold;
    private Boolean isItalic;
    private String underline;

    private String alignment;
    private Integer indentationFirstLine;
    private Integer spacingBetween;
    private Integer indentationLeft;
    private Integer indentationRight;
    private Integer spacingBefore;
    private Integer spacingAfter;
}
