package csit.semit.semitchecker.serviceenums;

import csit.semit.semitchecker.errorschecking.CheckParams;
import lombok.Getter;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Getter
public enum StandardHeadings {
    //ABSTRACT("referat"),
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

    public String getHeadingLocalized(CheckParams checkParams) {
        try {
            return ResourceBundle.getBundle("resourcesbundles/docskeywords/docskeywords",
                    checkParams.getLocaleDoc()).getString(getHeading());
        } catch (MissingResourceException e) {
            return getHeading();
        }
    }

    public static String[] getAllHeadingsLocalized(CheckParams checkParams) {
        String[] headings = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            headings[i] = values()[i].getHeadingLocalized(checkParams);
        }
        return headings;
    }
}
