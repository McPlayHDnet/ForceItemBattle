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
     * The MiniMessage colour a finishing place is shown in: gold, silver and bronze for the podium,
     * plain white for everyone else.
     */
    public static String placeColor(int place) {
        return switch (place) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<red>";
            default -> "<white>";
        };
    }
}
