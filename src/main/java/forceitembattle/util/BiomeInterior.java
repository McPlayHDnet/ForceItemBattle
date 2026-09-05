package forceitembattle.util;

/**
 * Walks from a point on a biome's rim to a point in its middle, given only a membership test.
 *
 * <p>{@code locateNearestBiome} answers a different question than a locator asks. It walks a
 * 32-block lattice outwards from the player and returns the <em>first</em> sample that matches,
 * with candidate heights ordered by distance from the player's own Y, so what comes back is the
 * near edge of the region and its shallowest fringe at that. For a surface biome that is the right
 * answer — you have arrived when you can see it, and {@code BiomeNoteLocator} wants exactly this.
 * For {@code sulfur_caves} it is the worst point in the blob. That biome is paint over whatever the
 * ordinary carvers happened to hollow out ({@code cave}, {@code cave_extra_underground},
 * {@code canyon}), so the biome existing somewhere is no promise a cave does — and at the rim,
 * where the region is a few cells thick, frequently none does. That is what "digging down there is
 * weird" means: the dig lands in the biome and in solid rock.
 *
 * <p>Bukkit exposes no "which biome is at this exact point" call, but a search that cannot leave
 * its origin is one — see {@link BiomeSearch#contains}. That membership test is all the geometry
 * here needs, so taking it as a {@link Probe} keeps this module headless and testable.
 *
 * <p>This finds the middle of the <em>biome</em>, which is a better place to dig than the rim but
 * still not a promise of open air. Only real blocks can settle that, which is {@link CaveScan}.
 */
public final class BiomeInterior {

    /** Biomes are stored per 4×4×4 cell, so a finer horizontal step than this only costs time. */
    private static final int HORIZONTAL_STEP = 16;

    /** Cave biomes are broad and thin, so height is measured more finely than width. */
    private static final int VERTICAL_STEP = 4;

    /** Caps on one march. Sulfur cave regions are ribbons, not continents. */
    private static final int MAX_HORIZONTAL_REACH = 24;  // steps, so 384 blocks each way
    private static final int MAX_VERTICAL_REACH = 24;    // steps, so 96 blocks each way

    /** Re-centring from the point the last pass found. Past three it has stopped moving. */
    private static final int PASSES = 3;

    private BiomeInterior() {
    }

    /** Whether a single point is inside the biome being searched for. */
    @FunctionalInterface
    public interface Probe {
        boolean contains(int x, int y, int z);
    }

    public record Point(int x, int y, int z) {
    }

    /**
     * The deepest point inside the blob {@code start} sits on, or {@code start} itself if the walk
     * cannot improve on it.
     *
     * <p>Every step is re-probed before it is taken, and a start the probe rejects is returned
     * unchanged. That is the fail-safe: if the membership test is ever wrong about a point the
     * biome search just handed us, this degrades to the rim point the locator used before rather
     * than to a confident lie.
     */
    public static Point centre(Point start, Probe probe) {
        if (!probe.contains(start.x(), start.y(), start.z())) {
            return start;
        }

        Point at = start;
        for (int pass = 0; pass < PASSES; pass++) {
            Point next = recentre(at, probe);
            if (next.equals(at)) {
                return at;
            }
            at = next;
        }
        return at;
    }

    /**
     * One pass: centre on X, then on Z from the new X, then on Y from both. Sequential rather than
     * simultaneous because a ribbon running diagonally has no meaningful width along either axis
     * until you are on its spine.
     */
    private static Point recentre(Point at, Probe probe) {
        Point centred = at;
        centred = shift(centred, probe, drift(probe, centred, 1, 0, 0, HORIZONTAL_STEP, MAX_HORIZONTAL_REACH), 0, 0);
        centred = shift(centred, probe, 0, 0, drift(probe, centred, 0, 0, 1, HORIZONTAL_STEP, MAX_HORIZONTAL_REACH));
        centred = shift(centred, probe, 0, drift(probe, centred, 0, 1, 0, VERTICAL_STEP, MAX_VERTICAL_REACH), 0);
        return centred;
    }

    /**
     * Moves, unless the midpoint has left the blob — which a bent ribbon's midpoint can, and a
     * dig spot outside the biome is the bug this whole module exists to fix.
     */
    private static Point shift(Point from, Probe probe, int dx, int dy, int dz) {
        if (dx == 0 && dy == 0 && dz == 0) {
            return from;
        }
        Point moved = new Point(from.x() + dx, from.y() + dy, from.z() + dz);
        return probe.contains(moved.x(), moved.y(), moved.z()) ? moved : from;
    }

    /** How far to move along one axis to sit halfway between the blob's two edges. */
    private static int drift(Probe probe, Point from, int dx, int dy, int dz, int step, int maxSteps) {
        int forward = reach(probe, from, dx, dy, dz, step, maxSteps);
        int back = reach(probe, from, -dx, -dy, -dz, step, maxSteps);
        return (forward - back) * step / 2;
    }

    /** Steps taken in one direction before the probe says we have left the biome. */
    private static int reach(Probe probe, Point from, int dx, int dy, int dz, int step, int maxSteps) {
        int steps = 0;
        while (steps < maxSteps) {
            int next = (steps + 1) * step;
            if (!probe.contains(from.x() + dx * next, from.y() + dy * next, from.z() + dz * next)) {
                break;
            }
            steps++;
        }
        return steps;
    }
}
