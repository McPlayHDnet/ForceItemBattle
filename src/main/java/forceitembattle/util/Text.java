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
     * Makes a string safe to drop inside a single-quoted MiniMessage tag argument, as in
     * {@code <hover:show_text:'...'>}.
     *
     * <p><b>An unescaped apostrophe does not merely lose the hover — it loses the whole message.</b>
     * MiniMessage closes the argument at the first {@code '}, fails to parse what follows as a tag,
     * and falls back to emitting the entire markup as literal text, so the player sees
     * {@code <hover:show_text:'<dark_aqua>That's a Rock...} in chat. Two achievement titles
     * ("That's a Rock, Jim" and "It's so empty") did exactly that.
     *
     * <p>Only for tag <em>arguments</em>. Text placed in a message body needs none of this — an
     * apostrophe is an ordinary character there, which is why the same title renders fine outside
     * the hover in the very same line.
     */
    public static String tagArgument(String value) {
        // Backslash first: escaping the quotes first would then double their new backslashes.
        return value.replace("\\", "\\\\").replace("'", "\\'");
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
