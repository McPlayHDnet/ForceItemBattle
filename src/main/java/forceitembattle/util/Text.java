package forceitembattle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component of(String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /**
     * For single-quoted MiniMessage tag arguments only: an unescaped apostrophe makes MiniMessage print
     * the whole message as literal markup, not just lose the hover.
     */
    public static String tagArgument(String value) {
        // Backslash first: escaping the quotes first would then double their new backslashes.
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static String placeColor(int place) {
        return switch (place) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<red>";
            default -> "<white>";
        };
    }
}
