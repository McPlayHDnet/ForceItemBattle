package forceitembattle.util;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * Turns generated chunks into a dig spot that has actually been looked at.
 *
 * <p>{@link BiomeInterior} gets the locator into the middle of the biome, which is as far as the
 * noise oracle can take it — the biome is paint over whatever the carvers hollowed out, so it is no
 * promise of open air anywhere. Only real blocks settle that, and real blocks mean generating the
 * chunks. This is the only part of the locator that costs anything.
 *
 * <p>Two things are worth finding, in this order:
 *
 * <ol>
 *   <li><b>Sulfur reaching the surface.</b> Two different things put it there and both are the
 *       answer the locator wants. One is {@code minecraft:rooted_sulfur_spring}, a
 *       {@code root_system} feature that starts under a cave ceiling and grows a radius-3 column
 *       of {@code minecraft:sulfur} up to 184 blocks to the surface before placing one of the ten
 *       {@code spring/sulfur_spring_*} templates — so it is <em>wired</em> to the cave, and the
 *       column is a shaft someone already dug. The other is the cave system itself surfacing: the
 *       overworld surface rules paint {@code sulfur} and {@code cinnabar} as the walls of a sulfur
 *       cave (on a {@code sulfur_cave_gradient} noise), so wherever a ravine or a cliff cuts the
 *       biome open, those same blocks top the column with no spring anywhere near. Digging where
 *       the sulfur is works for both, which is why they share a find — but only one of them is a
 *       spring, so nothing here says the word.
 *   <li><b>Failing that, the roomiest cave air in the biome.</b> Still a real cavity at a real
 *       depth, which is all the old rim point never was.
 * </ol>
 *
 * <p>Takes snapshots rather than chunks because snapshots are safe to read off the main thread,
 * which is where the scanning belongs — and because an interface is something a test can hand a
 * hand-built world to.
 */
public final class CaveScan {

    /**
     * What a sulfur cave puts at the surface, whether by spring or by being cut open. All four
     * occur down in the cave as well — they are its walls and its decoration — but nothing else
     * in the overworld raises them into the top of a column, so at the surface they are a tell.
     * Water and magma are in the spring templates too and are left out for the obvious reason.
     */
    private static final Set<Material> SURFACE_SULFUR = EnumSet.of(
            Material.SULFUR,
            Material.POTENT_SULFUR,
            Material.SULFUR_SPIKE,
            Material.CINNABAR);

    /** How far above and below the top block to look. Covers either heightmap convention. */
    private static final int SURFACE_BAND = 4;

    /** How far above and below the biome centre to look for cave air. */
    private static final int VERTICAL_WINDOW = 48;

    /** A pocket shorter than this is a crack, not somewhere to land. */
    private static final int MIN_HEADROOM = 3;

    private CaveScan() {
    }

    public enum Find {
        /** Sulfur in the top of a column: a spring's root system, or the cave cut open. */
        SURFACE,
        /** Open cave air inside the biome. */
        CAVE
    }

    public record Target(int x, int y, int z, Find find) {
    }

    /**
     * The best dig spot in these chunks, or {@code null} if none of them turned out to hold one.
     *
     * <p>Ties break towards {@code centre}: the locator has already been pointing the player
     * somewhere for as long as the chunks took to generate, and a target that jumps 60 blocks
     * sideways at the end reads as a bug even when it is a better hole.
     */
    @Nullable
    public static Target scan(List<ChunkSnapshot> snapshots, Biome biome,
                              int centreX, int centreY, int centreZ, int minHeight, int maxHeight) {
        Target surface = null;
        Target cave = null;

        for (ChunkSnapshot snapshot : snapshots) {
            surface = closer(surface, findSurfaceSulfur(snapshot, minHeight, maxHeight, centreX, centreZ), centreX, centreZ);
            if (surface != null) {
                continue; // sulfur you can walk to beats a hole; stop paying for the deep scan
            }
            if (!snapshot.contains(biome)) {
                continue;
            }
            cave = closer(cave, findCave(snapshot, biome, centreY, minHeight, maxHeight, centreX, centreZ), centreX, centreZ);
        }

        return surface != null ? surface : cave;
    }

    /** Sulfur cave blocks sitting in the top of a column, if this chunk has any. */
    @Nullable
    private static Target findSurfaceSulfur(ChunkSnapshot snapshot, int minHeight, int maxHeight, int centreX, int centreZ) {
        Target best = null;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = snapshot.getHighestBlockYAt(x, z);
                int from = Math.min(top + 1, maxHeight - 1);
                int to = Math.max(top - SURFACE_BAND, minHeight);

                for (int y = from; y >= to; y--) {
                    if (!SURFACE_SULFUR.contains(snapshot.getBlockType(x, y, z))) {
                        continue;
                    }
                    Target found = new Target(worldX(snapshot, x), y, worldZ(snapshot, z), Find.SURFACE);
                    best = closer(best, found, centreX, centreZ);
                    break;
                }
            }
        }

        return best;
    }

    /** The tallest pocket of cave air inside the biome, nearest the centre. */
    @Nullable
    private static Target findCave(ChunkSnapshot snapshot, Biome biome, int centreY,
                                   int minHeight, int maxHeight, int centreX, int centreZ) {
        int from = Math.max(centreY - VERTICAL_WINDOW, minHeight);
        int to = Math.min(centreY + VERTICAL_WINDOW, maxHeight - 1);

        Target best = null;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int floor = -1;
                int headroom = 0;

                for (int y = from; y <= to; y++) {
                    if (!isEmpty(snapshot.getBlockType(x, y, z))) {
                        floor = -1;
                        headroom = 0;
                        continue;
                    }
                    if (headroom == 0) {
                        // Only the foot of a pocket is worth a biome lookup; the rest of it is the
                        // same 4-block cell, and this loop runs a quarter of a million times.
                        if (snapshot.getBiome(x, y, z) != biome) {
                            continue;
                        }
                        floor = y;
                    }
                    headroom++;

                    if (headroom == MIN_HEADROOM) { // once per pocket, at the moment it qualifies
                        Target found = new Target(worldX(snapshot, x), floor, worldZ(snapshot, z), Find.CAVE);
                        best = closer(best, found, centreX, centreZ);
                    }
                }
            }
        }

        return best;
    }

    /** Carvers leave cave air, not air; a scan that only knows one of them finds no caves at all. */
    private static boolean isEmpty(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR;
    }

    private static int worldX(ChunkSnapshot snapshot, int x) {
        return (snapshot.getX() << 4) + x;
    }

    private static int worldZ(ChunkSnapshot snapshot, int z) {
        return (snapshot.getZ() << 4) + z;
    }

    @Nullable
    private static Target closer(@Nullable Target current, @Nullable Target candidate, int centreX, int centreZ) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return distanceSquared(candidate, centreX, centreZ) < distanceSquared(current, centreX, centreZ)
                ? candidate
                : current;
    }

    private static long distanceSquared(Target target, int centreX, int centreZ) {
        long dx = (long) target.x() - centreX;
        long dz = (long) target.z() - centreZ;
        return dx * dx + dz * dz;
    }
}
