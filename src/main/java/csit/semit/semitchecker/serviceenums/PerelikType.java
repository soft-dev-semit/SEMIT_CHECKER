package csit.semit.semitchecker.serviceenums;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum PerelikType {
    ListMarkedSTD("[a-zа-яіїє\"]", ";", ":"),
    ListNumericWithBracket("[a-zа-яіїє\"]", ";", ":"),
    ListNumeric1("[A-ZА-Я}ІЇЄ\"]", ".", "."),
    ListNumericAua("[a-zа-яіїє\"]", ";", ":"),
    ListNumericAen("[a-z\"]", ";", ":");

    private final String maskFirstSymbol;
    private final String lastSymbol;
    private final String prevSentSymbol;

    PerelikType(String maskFirstSymbol, String lastSymbol, String prevSentSymbol) {
        this.maskFirstSymbol = maskFirstSymbol;
        this.lastSymbol = lastSymbol;
        this.prevSentSymbol = prevSentSymbol;
    }

    public String getMaskFirstSymbol() {
        return maskFirstSymbol;
    }

    public String getLastSymbol() {
        return lastSymbol;
    }

    public String getPrevSentSymbol() {
        return prevSentSymbol;
    }

    public static @Nullable PerelikType getPerelikTypeByStyleName(String styleName) {
        for (PerelikType perelik: PerelikType.values()) {
            if (perelik.name().equals(styleName)) {
                return perelik;
            }
        }
        return null;
    }
}
