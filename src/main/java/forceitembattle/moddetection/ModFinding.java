package forceitembattle.moddetection;

public enum ModFinding {

    XAERO_MINIMAP("<gold>Xaero's Minimap <gray>detected for <yellow>%s<gray> - it got automatically disabled."),
    FREECAM_INSTALLED("<gold>Freecam <gray>detected for <yellow>%s<gray>."),
    FREECAM_IN_USE("<gold>Freecam <gray>in use by <yellow>%s<gray>.");

    private final String template;

    ModFinding(String template) {
        this.template = template;
    }

    public String message(String playerName) {
        return this.template.formatted(playerName);
    }
}
