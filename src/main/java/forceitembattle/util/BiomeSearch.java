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

    /**
     * Wider than the world is tall, which is what collapses a search to a single height. See
     * {@link #contains}.
     */
    private static final int WORLD_SPANNING_INTERVAL = 4096;

    private BiomeSearch() {
    }

    @Nullable
    public static Biome resolve(NamespacedKey key) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.BIOME)
                .get(key);
    }

    /**
     * The nearest point of the biome to {@code origin} — which is, by construction, on the biome's
     * near edge. Right for anything you only have to arrive at; see {@link BiomeInterior} for why
     * an underground biome wants more than this.
     */
    @Nullable
    public static Location nearest(Location origin, Biome biome) {
        BiomeSearchResult result = origin.getWorld().locateNearestBiome(origin, SEARCH_RADIUS, biome);
        return result != null ? result.getLocation() : null;
    }

    /**
     * Whether that exact block sits in the biome.
     *
     * <p>A search that cannot leave its origin is a point test, and that is the whole trick here:
     * {@code radius / horizontalInterval} floors to zero, which leaves the horizontal spiral with
     * only the origin column, and a vertical interval wider than the world leaves exactly one
     * candidate height. One noise sample, and no chunk is loaded or generated — this reads the
     * same biome source {@code /locate biome} does, not the world.
     *
     * <p>Verified against the 26.2 server: {@code spiralAround(ZERO, 0, …)} yields one position
     * and {@code outFromOrigin(y, min, max, 4096)} yields {@code [y]}. Both are internals, so if a
     * Minecraft update ever breaks the collapse this starts answering {@code false} everywhere,
     * and {@link BiomeInterior#centre} degrades to returning the rim point it was given.
     */
    public static boolean contains(World world, int x, int y, int z, Biome biome) {
        Location at = new Location(world, x, y, z);
        return world.locateNearestBiome(at, 0, 1, WORLD_SPANNING_INTERVAL, biome) != null;
    }

    /** A {@link BiomeInterior.Probe} over one world's biome source. */
    public static BiomeInterior.Probe probe(World world, Biome biome) {
        return (x, y, z) -> contains(world, x, y, z, biome);
    }

    /**
     * The middle of the biome region {@code origin} is standing on the edge of. Cheap — a few
     * hundred noise samples, no chunk generation — and never worse than {@code origin}.
     */
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
