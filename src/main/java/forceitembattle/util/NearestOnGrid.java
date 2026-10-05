package forceitembattle.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Works around MC-138887, still in 26.2: the server's structure search returns the first hit in ring
 * order, not the nearest, off by up to one grid step (544 blocks for spacing 34). Probing one region
 * at a time with {@code radius = 0} and comparing real distances fixes the ordering. Samples run
 * nearest-first, so a sweep cut short still holds the best answer from the closest ground.
 */
public final class NearestOnGrid {

    /** Where a structure actually is, in blocks. */
    public record Spot(int x, int y, int z) {
    }

    /** One chunk to ask about. Probing it answers for the whole region containing it. */
    public record Sample(int chunkX, int chunkZ) {
    }

    /** The structure generating in the region around this chunk, or {@code null}. */
    @FunctionalInterface
    public interface Probe {
        @Nullable
        Spot at(int chunkX, int chunkZ);
    }

    private final int originX;
    private final int originZ;
    private final List<Sample> samples;
    private final double slack;

    private int next;
    private @Nullable Spot best;
    private long bestDistance = Long.MAX_VALUE;

    /**
     * @param radiusBlocks  how far out to sweep before giving up
     * @param spacingChunks the structure set's {@code spacing}; under the real value only repeats probes,
     *                      over it skips whole regions
     */
    public NearestOnGrid(int originX, int originZ, int radiusBlocks, int spacingChunks) {
        this.originX = originX;
        this.originZ = originZ;
        this.samples = grid(originX, originZ, radiusBlocks, spacingChunks);
        // A structure's distance from its sample: the region diagonal plus a chunk. Erring high only costs probes.
        this.slack = spacingChunks * 16.0 * Math.sqrt(2.0) + 16.0;
    }

    /** The sample chunks, nearest first. */
    private static List<Sample> grid(int originX, int originZ, int radiusBlocks, int stepChunks) {
        int originChunkX = originX >> 4;
        int originChunkZ = originZ >> 4;
        int reach = Math.max(1, ceilDiv(radiusBlocks / 16, stepChunks));
        long radiusSquared = (long) radiusBlocks * radiusBlocks;

        List<Sample> samples = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                Sample sample = new Sample(originChunkX + dx * stepChunks, originChunkZ + dz * stepChunks);
                if (distanceSquared(originX, originZ, centreOf(sample.chunkX()), centreOf(sample.chunkZ())) <= radiusSquared) {
                    samples.add(sample);
                }
            }
        }

        samples.sort(Comparator.comparingLong(sample ->
                distanceSquared(originX, originZ, centreOf(sample.chunkX()), centreOf(sample.chunkZ()))));
        return samples;
    }

    /** @return whether the sweep is finished */
    public boolean advance(int budget, Probe probe) {
        int limit = Math.min(this.next + budget, this.samples.size());

        for (; this.next < limit; this.next++) {
            Sample sample = this.samples.get(this.next);

            if (this.settled(sample)) {
                this.next = this.samples.size();
                return true;
            }

            Spot spot = probe.at(sample.chunkX(), sample.chunkZ());
            if (spot == null) {
                continue;
            }
            // The whole point: judged on where the structure is, not on which cell found it.
            long distance = distanceSquared(this.originX, this.originZ, spot.x(), spot.z());
            if (distance < this.bestDistance) {
                this.bestDistance = distance;
                this.best = spot;
            }
        }

        return this.done();
    }

    /** Nearest-first samples plus {@link #slack} bound what's left, so a near find stops the sweep early. */
    private boolean settled(Sample sample) {
        if (this.best == null) {
            return false;
        }
        double reach = Math.sqrt(distanceSquared(this.originX, this.originZ,
                centreOf(sample.chunkX()), centreOf(sample.chunkZ()))) - this.slack;
        return Math.sqrt(this.bestDistance) <= reach;
    }

    public boolean done() {
        return this.next >= this.samples.size();
    }

    /** The nearest structure found so far, or {@code null} if nothing has turned up. */
    @Nullable
    public Spot best() {
        return this.best;
    }

    /** How many samples the sweep still has to get through. For budgeting and for tests. */
    public int remaining() {
        return this.samples.size() - this.next;
    }

    private static int centreOf(int chunkCoordinate) {
        return (chunkCoordinate << 4) + 8;
    }

    private static int ceilDiv(int value, int divisor) {
        return -Math.floorDiv(-value, divisor);
    }

    private static long distanceSquared(int fromX, int fromZ, int toX, int toZ) {
        long dx = (long) toX - fromX;
        long dz = (long) toZ - fromZ;
        return dx * dx + dz * dz;
    }
}
