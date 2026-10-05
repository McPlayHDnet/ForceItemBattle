package forceitembattle.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

public final class StructureSearch {

    /** Beyond this the server's own search is used: MC-138887's error is bounded by spacing, negligible at this range. */
    public static final int PRECISE_RADIUS = 2500;

    private StructureSearch() {
    }

    /**
     * {@code radius = 0} is load-bearing: the server's ring loops run once, against the region holding
     * the given chunk (verified on 26.2). Generates no chunks, which keeps hundreds of probes affordable.
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
