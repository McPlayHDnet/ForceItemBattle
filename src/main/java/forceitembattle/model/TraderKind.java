package forceitembattle.model;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;

@Getter
public enum TraderKind {

    WANDERING("<green>", "Wandering Trader", Color.LIME, NamedTextColor.GREEN),
    SPECIAL("<light_purple>", "Special Trader", Color.FUCHSIA, NamedTextColor.LIGHT_PURPLE);

    private final String color;
    private final String displayName;

    /** Colour of the particle line drawn to this trader on spawn. */
    private final Color particleColor;

    /**
     * Outline colour of this trader's glow. Read off the scoreboard team the entity sits on, so it
     * must be one of the sixteen named colours — arbitrary RGB isn't available for living entities.
     */
    private final NamedTextColor glowColor;

    TraderKind(String color, String displayName, Color particleColor, NamedTextColor glowColor) {
        this.color = color;
        this.displayName = displayName;
        this.particleColor = particleColor;
        this.glowColor = glowColor;
    }

    public String coloredName() {
        return this.color + this.displayName;
    }

    public String boldColoredName() {
        return this.color + "<b>" + this.displayName + "</b>";
    }
}
