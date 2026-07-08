package forceitembattle.settings;

public enum QuickieMode {

    DISABLED("Disabled"),
    EARLY("Early only"),
    EARLY_MID("Early + Mid");

    private final String displayName;

    QuickieMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }

    public QuickieMode next() {
        QuickieMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public QuickieMode previous() {
        QuickieMode[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static QuickieMode fromOrdinal(int ordinal) {
        QuickieMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return DISABLED;
        return values[ordinal];
    }
}
