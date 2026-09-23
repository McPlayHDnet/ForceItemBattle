package forceitembattle.ceremony;

import forceitembattle.model.ForceItem;
import forceitembattle.model.Rarity;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Material;

final class StagePalette {

    static final Color JOKER = Color.fromRGB(0xFF5555);
    static final Color CARD = Color.fromARGB(0xE0, 0x10, 0x10, 0x14);
    static final Color NONE = Color.fromARGB(0, 0, 0, 0);

    private StagePalette() {
    }

    /** The B2B rarity's colour, else red for a Joker, else null for no glow at all. */
    @Nullable
    static Color glowOf(ForceItem item) {
        Rarity rarity = StageTimeline.rarityOf(item);
        if (rarity != null) {
            return glowOf(rarity);
        }
        return item.usedSkip() ? JOKER : null;
    }

    // The first stop of each rarity's gradient in Rarity's labels.
    static Color glowOf(Rarity rarity) {
        return switch (rarity) {
            case RARE -> Color.fromRGB(0x5555FF);
            case EPIC -> Color.fromRGB(0xAA00AA);
            case LEGENDARY -> Color.fromRGB(0xFFAA00);
            case RNGESUS -> Color.fromRGB(0xE41EBC);
            case EXTRAORDINARY -> Color.fromRGB(0x73FF00);
        };
    }

    /** The card's backdrop, tinted towards the glow so a rare find reads as rare before it is read. */
    static Color cardFor(@Nullable Color glow) {
        if (glow == null) {
            return CARD;
        }
        return Color.fromARGB(CARD.getAlpha(),
                mix(CARD.getRed(), glow.getRed()),
                mix(CARD.getGreen(), glow.getGreen()),
                mix(CARD.getBlue(), glow.getBlue()));
    }

    static Material stepOf(int place) {
        return switch (place) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            default -> Material.COPPER_BLOCK;
        };
    }

    static Color fireworkOf(int place) {
        return switch (place) {
            case 1 -> Color.fromRGB(0xFFAA00);
            case 2 -> Color.fromRGB(0xD0D0D0);
            default -> Color.fromRGB(0xC06040);
        };
    }

    private static int mix(int base, int tint) {
        return (base * 2 + tint) / 3;
    }
}
