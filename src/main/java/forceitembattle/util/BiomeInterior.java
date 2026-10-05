package forceitembattle.util;

/**
 * {@code locateNearestBiome} returns the near rim, the worst point to dig into a cave biome, which is
 * paint over whatever the carvers hollowed out. Headless: the membership test is a {@link Probe}.
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

    /** A start the probe rejects is returned unchanged, so a wrong membership test degrades to the rim point. */
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

    /** Sequential rather than simultaneous: a diagonal ribbon has no meaningful width until you are on its spine. */
    private static Point recentre(Point at, Probe probe) {
        Point centred = at;
        centred = shift(centred, probe, drift(probe, centred, 1, 0, 0, HORIZONTAL_STEP, MAX_HORIZONTAL_REACH), 0, 0);
        centred = shift(centred, probe, 0, 0, drift(probe, centred, 0, 0, 1, HORIZONTAL_STEP, MAX_HORIZONTAL_REACH));
        centred = shift(centred, probe, 0, drift(probe, centred, 0, 1, 0, VERTICAL_STEP, MAX_VERTICAL_REACH), 0);
        return centred;
    }

    /** Refuses a move whose midpoint left the blob, which a bent ribbon's can. */
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
