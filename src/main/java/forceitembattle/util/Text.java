package forceitembattle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.shadowColor())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.reset())
                    .resolver(StandardTags.newline())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.clickEvent())
                    .resolver(StandardTags.hoverEvent())
                    .resolver(StandardTags.translatable())
                    .build())
            .build();

    private Text() {
    }

    public static MiniMessage mm() {
        return MINI_MESSAGE;
    }

    public static Component of(String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }
}