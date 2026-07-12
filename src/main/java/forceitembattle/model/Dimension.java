package forceitembattle.model;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
public enum Dimension {

    OVERWORLD(World.Environment.NORMAL, "world", "overworld", "<green>"),
    NETHER(World.Environment.NETHER, "world_nether", "nether", "<red>"),
    END(World.Environment.THE_END, "world_the_end", "end", "<dark_purple>");

    private final World.Environment environment;
    private final String worldName;
    private final String displayName;
    private final String color;

    Dimension(World.Environment environment, String worldName, String displayName, String color) {
        this.environment = environment;
        this.worldName = worldName;
        this.displayName = displayName;
        this.color = color;
    }

    public String coloredName() {
        return this.color + this.displayName;
    }

    @Nullable
    public World world() {
        return Bukkit.getWorld(this.worldName);
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
