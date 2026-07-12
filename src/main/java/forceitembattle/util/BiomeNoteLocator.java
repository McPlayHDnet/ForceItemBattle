package forceitembattle.util;

import forceitembattle.model.BiomeNote;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.BiomeSearchResult;
import org.jetbrains.annotations.Nullable;

public final class BiomeNoteLocator {

    public static final int SEARCH_RADIUS = 6400;

    private static final String[] DIRECTIONS = {
            "south", "south-east", "east", "north-east",
            "north", "north-west", "west", "south-west"
    };

    private BiomeNoteLocator() {
    }

    @Nullable
    public static Location locate(BiomeNote note, Location origin) {
        Biome biome = BiomeSearch.resolve(note.getBiomeKey());
        return biome != null ? BiomeSearch.nearest(origin, biome) : null;
    }

    /**
     * Eight-point compass bearing from origin to target, matching the datapack's sectors.
     */
    public static String direction(Location origin, Location target) {
        double dx = target.getX() - origin.getX();
        double dz = target.getZ() - origin.getZ();

        double degrees = Math.toDegrees(Math.atan2(dx, dz));
        int sector = (int) Math.round(((degrees + 360.0D) % 360.0D) / 45.0D) % 8;
        return DIRECTIONS[sector];
    }

    /**
     * Horizontal distance, rounded to the nearest 100.
     */
    public static int distance(Location origin, Location target) {
        double dx = target.getX() - origin.getX();
        double dz = target.getZ() - origin.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        return (int) (Math.round(distance / 100.0D) * 100L);
    }

    public static boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }
}
