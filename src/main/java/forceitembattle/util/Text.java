package forceitembattle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static MiniMessage mm() {
        return MINI_MESSAGE;
    }

    public static Component of(String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }
}
