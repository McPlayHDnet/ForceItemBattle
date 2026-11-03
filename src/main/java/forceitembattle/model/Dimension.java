package forceitembattle.model;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
public enum Dimension {

    OVERWORLD(World.Environment.NORMAL, "overworld", "<green>"),
    NETHER(World.Environment.NETHER, "nether", "<red>"),
    END(World.Environment.THE_END, "end", "<dark_purple>");

    private final World.Environment environment;
    private final String displayName;
    private final String color;

    Dimension(World.Environment environment, String displayName, String color) {
        this.environment = environment;
        this.displayName = displayName;
        this.color = color;
    }

    public String coloredName() {
        return this.color + this.displayName;
    }

    /** By environment, not name: level-name is server-configurable. The default worlds are listed first. */
    @Nullable
    public World world() {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == this.environment)
                .findFirst()
                .orElse(null);
    }

    public boolean isOverworld() {
        return this == OVERWORLD;
    }

    public static Dimension of(@Nullable World world) {
        if (world == null) {
            return OVERWORLD;
        }
        for (Dimension dimension : values()) {
            if (dimension.environment == world.getEnvironment()) {
                return dimension;
            }
        }
        return OVERWORLD;
    }

    public static Dimension of(Player player) {
        return of(player.getWorld());
    }

    public static boolean isOverworld(Player player) {
        return of(player).isOverworld();
    }
}
