package forceitembattle.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the genuinely nearest structure by asking one region at a time and comparing real
 * distances, instead of trusting the order the server searches in.
 *
 * <p><b>The bug this exists for is <a href="https://bugs.mojang.com/browse/MC-138887">MC-138887</a>,
 * and it is still in 26.2.</b> {@code ChunkGenerator#getNearestGeneratedStructure} walks outward in
 * square rings of regions:
 *
 * <pre>{@code
 * for dx in -radius..radius:
 *     edge = (dx == -radius || dx == radius)
 *     for dz in -radius..radius step (edge ? 1 : 2*radius):
 *         hit = getStructureGeneratingAt(originChunk + spacing*dx, ...)
 *         if (hit != null) return hit          // first in iteration order, not the nearest
 * }</pre>
 *
 * <p>Two things go wrong there and both are early returns. The inner loop hands back whichever
 * cell of the ring it happens to visit first, and the outer search hands back the first ring that
 * produces anything at all — so a structure at the near corner of ring 1 loses to one at the far
 * corner of ring 0. Trial chambers and trail ruins both have {@code spacing: 34}, which makes one
 * grid step 544 blocks, and that is the size of the error: a chamber 300 blocks away routinely
 * loses to one at 700.
 *
 * <p>The fix falls out of the same loops. At {@code radius = 0} they run exactly once and probe
 * precisely the one region containing the origin chunk, which turns Bukkit's
 * {@code locateNearestStructure} into the "what is in <em>this</em> region" primitive the API
 * otherwise never offers. Sample a grid of origin chunks, probe each one, and keep the minimum by
 * <em>actual</em> distance. The ordering that was wrong is then ours, and it is right.
 *
 * <p>Samples are ordered nearest-first, which is what makes stopping early safe: probing is
 * budgeted across ticks, so a search cut short still holds the best answer from the closest ground.
 * Headless on purpose — the ordering and the picking are the parts worth testing, and neither
 * needs a server.
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

    private int next;
    private @Nullable Spot best;
    private long bestDistance = Long.MAX_VALUE;

    /**
     * @param radiusBlocks how far out to sweep exhaustively
     * @param stepChunks   the sample grid's pitch. Must be no larger than the structure set's
     *                     {@code spacing}, or whole regions fall between samples and their
     *                     structures are never seen. Smaller than the spacing only costs repeat
     *                     probes, which land on the server's own structure cache and agree with
     *                     each other, so erring low is free and erring high is not.
     */
    public NearestOnGrid(int originX, int originZ, int radiusBlocks, int stepChunks) {
        this.originX = originX;
        this.originZ = originZ;
        this.samples = grid(originX, originZ, radiusBlocks, stepChunks);
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

    /**
     * Probes at most {@code budget} more samples.
     *
     * @return whether the sweep is finished
     */
    public boolean advance(int budget, Probe probe) {
        int limit = Math.min(this.next + budget, this.samples.size());

        for (; this.next < limit; this.next++) {
            Sample sample = this.samples.get(this.next);
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
