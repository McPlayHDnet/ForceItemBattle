package forceitembattle.model;

import lombok.Getter;
import org.bukkit.Color;

@Getter
public enum TraderKind {

    WANDERING("<green>", "Wandering Trader", Color.LIME),
    SPECIAL("<light_purple>", "Special Trader", Color.FUCHSIA);

    private final String color;
    private final String displayName;

    /** Colour of the particle line drawn to this trader on spawn. */
    private final Color particleColor;

    TraderKind(String color, String displayName, Color particleColor) {
        this.color = color;
        this.displayName = displayName;
        this.particleColor = particleColor;
    }

    public String coloredName() {
        return this.color + this.displayName;
    }

    public String boldColoredName() {
        return this.color + "<b>" + this.displayName + "</b>";
    }
}
