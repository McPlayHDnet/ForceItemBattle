package forceitembattle.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.BiomeSearchResult;
import org.jetbrains.annotations.Nullable;

public final class BiomeSearch {

    public static final int SEARCH_RADIUS = 6400;

    /** Wider than the world is tall, which collapses a search to a single height. */
    private static final int WORLD_SPANNING_INTERVAL = 4096;

    private BiomeSearch() {
    }

    @Nullable
    public static Biome resolve(NamespacedKey key) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BIOME)
                .get(key);
    }

    /** On the biome's near edge by construction; see {@link BiomeInterior} for underground biomes. */
    @Nullable
    public static Location nearest(Location origin, Biome biome) {
        BiomeSearchResult result = origin.getWorld().locateNearestBiome(origin, SEARCH_RADIUS, biome);
        return result != null ? result.getLocation() : null;
    }

    /**
     * A search that cannot leave its origin is a point test: one noise sample, no chunk loaded. Relies on
     * 26.2 internals; if an update breaks it this answers false and {@link BiomeInterior} returns the rim.
     */
    public static boolean contains(World world, int x, int y, int z, Biome biome) {
        Location at = new Location(world, x, y, z);
        return world.locateNearestBiome(at, 0, 1, WORLD_SPANNING_INTERVAL, biome) != null;
    }

    /** A {@link BiomeInterior.Probe} over one world's biome source. */
    public static BiomeInterior.Probe probe(World world, Biome biome) {
        return (x, y, z) -> contains(world, x, y, z, biome);
    }

    /** A few hundred noise samples, no chunk generation, and never worse than {@code origin}. */
    public static Location interior(Location origin, Biome biome) {
        World world = origin.getWorld();
        if (world == null) {
            return origin;
        }

        BiomeInterior.Point centre = BiomeInterior.centre(
                new BiomeInterior.Point(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()),
                probe(world, biome));

        return new Location(world, centre.x(), centre.y(), centre.z());
    }
}
