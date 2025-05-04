package csit.semit.semitchecker.serviceenums;

public enum PerelikType {
    ListMarkedSTD("[a-zа-яіїє\"]", ";", ":"),
    ListNumeric1("[А-яA-Z}ІЇЄ\"]", ".", "."),
    ListNumericA("[a-zа-яіїє\"]", ";", ":"),
    ListNumericWithBracket("[a-zа-яіїє\"]", ";", ":");

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
}
