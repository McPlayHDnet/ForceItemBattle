package forceitembattle.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Bukkit half of the structure search. {@link NearestOnGrid} owns the ordering; this owns the
 * one API call it rests on.
 */
public final class StructureSearch {

    /**
     * How far the exhaustive sweep reaches. Beyond this the plugin falls back to the server's own
     * wide search: the error MC-138887 causes is bounded by the structure set's spacing (544 blocks
     * for trial chambers and trail ruins), which matters a great deal at 300 blocks and very little
     * at eight thousand.
     */
    public static final int PRECISE_RADIUS = 2500;

    /**
     * The sample grid's pitch, in chunks. Below every spacing the plugin's locators care about —
     * trial chambers 34, trail ruins 34, {@code fib:antimatter_depths_portal} 112 — with room to
     * spare, because being under the real spacing is what makes the sweep complete. A locator
     * pointed at some denser structure set than these would have to lower it.
     */
    public static final int STEP_CHUNKS = 16;

    private StructureSearch() {
    }

    /**
     * A probe for one structure in one world.
     *
     * <p>{@code radius = 0} is the load-bearing argument. It makes the server's ring loops run
     * exactly once, against the single region containing the chunk handed in, so this answers
     * "what is in this region" rather than "what did the search stumble on first". Verified against
     * 26.2: the radius reaches {@code findNearestMapStructure} unclamped, and both of its loops are
     * {@code for (d = -radius; d <= radius; …)}.
     *
     * <p>Generates no chunks — {@code locateNearestStructure} is documented not to, and that is
     * what keeps a few hundred of these affordable.
     */
    public static NearestOnGrid.Probe probe(World world, Structure structure, int originY) {
        return (chunkX, chunkZ) -> {
            Location at = new Location(world, chunkX << 4, originY, chunkZ << 4);
            StructureSearchResult result = world.locateNearestStructure(at, structure, 0, false);
            if (result == null) {
                return null;
            }
            Location found = result.getLocation();
            return new NearestOnGrid.Spot(found.getBlockX(), found.getBlockY(), found.getBlockZ());
        };
    }

    /** The server's own wide search — the one MC-138887 is about, kept for beyond {@link #PRECISE_RADIUS}. */
    @Nullable
    public static Location wide(Location origin, Structure structure, int radiusInRings) {
        StructureSearchResult result = origin.getWorld()
                .locateNearestStructure(origin, structure, radiusInRings, false);
        return result != null ? result.getLocation() : null;
    }

    /** A found spot, back as a world location. */
    public static Location toLocation(World world, NearestOnGrid.Spot spot) {
        return new Location(world, spot.x(), spot.y(), spot.z());
    }
}
