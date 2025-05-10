package csit.semit.semitchecker.serviceenums;

import csit.semit.semitchecker.errorschecking.CheckParams;

import java.util.ResourceBundle;

public enum StandardHeadings {
    ABSTRACT("referat"),
    CONTENTS("zmist"),
    ABBREVIATIONS("abbr"),
    INTRODUCTION("vstup"),
    CONCLUSIONS("vysnovki"),
    REFERENCES("litra"),
    APPENDIX("dodatok");

    private final String heading;

    StandardHeadings(String heading) {
        this.heading = heading;
    }

    public String getHeading() {
        return heading;
    }

    public String getHeadingLocalized(CheckParams checkParams) {
        return ResourceBundle.getBundle("resourcesbundles/docskeywords/docskeywords",
                checkParams.getLocaleDoc()).getString(heading);
    }

    public static String[] getAllHeadingsLocalized(CheckParams checkParams) {
        String[] headings = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            headings[i] = values()[i].getHeadingLocalized(checkParams);
        }
        return headings;
    }
}
