package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.util.NearestOnGrid.Probe;
import forceitembattle.util.NearestOnGrid.Spot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The search order MC-138887 gets wrong.
 *
 * <p>Every case here is stated as a probe that answers however the test needs it to, so the bug
 * being fixed is reproducible without a world: the server's failure is that it trusts the order it
 * visits regions in, and the fix is that this compares real distances instead. Both are pure
 * arithmetic and neither needs Minecraft to demonstrate.
 */
class NearestOnGridTest {

    private static final int SPACING = 34;   // trial chambers and trail ruins both
    private static final int STEP = 16;      // StructureSearch.STEP_CHUNKS

    /** Nothing anywhere. */
    private static final Probe EMPTY = (chunkX, chunkZ) -> null;

    /** Answers with {@code spot} for the one region containing {@code inChunk}, floor-divided like the server does. */
    private static Probe regionHolding(int chunkX, int chunkZ, Spot spot) {
        int regionX = Math.floorDiv(chunkX, SPACING);
        int regionZ = Math.floorDiv(chunkZ, SPACING);
        return (probeX, probeZ) ->
                Math.floorDiv(probeX, SPACING) == regionX && Math.floorDiv(probeZ, SPACING) == regionZ
                        ? spot
                        : null;
    }

    private static Probe either(Probe first, Probe second) {
        return (chunkX, chunkZ) -> {
            Spot spot = first.at(chunkX, chunkZ);
            return spot != null ? spot : second.at(chunkX, chunkZ);
        };
    }

    private static Spot sweep(int originX, int originZ, Probe probe) {
        NearestOnGrid search = new NearestOnGrid(originX, originZ, 2500, STEP);
        while (!search.advance(64, probe)) {
            // keep going
        }
        return search.best();
    }

    @Nested
    @DisplayName("picking")
    class Picking {

        @Test
        @DisplayName("takes the actually nearest structure, not the first region that has one")
        void takesTheNearest() {
            // The bug, in one assertion. The server walks regions outward and returns the first
            // hit it meets, so a structure at 700 blocks in an earlier-visited region beats one at
            // 300 in a later-visited one. Distances here are chosen to match what the sulfur and
            // trial locators were actually reporting in game.
            Spot near = new Spot(300, 64, 0);
            Spot far = new Spot(-700, 64, 0);

            Spot best = sweep(0, 0, either(
                    regionHolding(far.x() >> 4, far.z() >> 4, far),
                    regionHolding(near.x() >> 4, near.z() >> 4, near)));

            assertEquals(near, best);
        }

        @Test
        @DisplayName("prefers the nearer of two structures whichever order they are probed in")
        void orderIndependent() {
            Spot near = new Spot(0, 64, 400);
            Spot far = new Spot(0, 64, -1400);
            Probe nearFirst = either(regionHolding(0, 25, near), regionHolding(0, -88, far));
            Probe farFirst = either(regionHolding(0, -88, far), regionHolding(0, 25, near));

            assertEquals(sweep(0, 0, nearFirst), sweep(0, 0, farFirst));
            assertEquals(near, sweep(0, 0, farFirst));
        }

        @Test
        @DisplayName("returns nothing when the sweep comes up empty")
        void findsNothing() {
            assertNull(sweep(0, 0, EMPTY));
        }

        @Test
        @DisplayName("measures from the origin to the structure, not to the chunk that found it")
        void measuresToTheStructure() {
            // A region is 34 chunks across, so the chunk that answers can be up to ~544 blocks from
            // the structure it reports. Judging by the probe position instead of the result is the
            // same class of mistake as the bug.
            Spot spot = new Spot(1200, 64, 0);
            NearestOnGrid search = new NearestOnGrid(0, 0, 2500, STEP);
            while (!search.advance(64, regionHolding(75, 0, spot))) {
                // keep going
            }

            assertEquals(spot, search.best());
        }
    }

    @Nested
    @DisplayName("sweeping")
    class Sweeping {

        @Test
        @DisplayName("never leaves a region unsampled")
        void coversEveryRegion() {
            // The one thing the step has to guarantee. If consecutive samples were ever further
            // apart than a structure set's spacing, whole regions would fall between them and
            // their structures would simply never be seen.
            List<Integer> columns = new ArrayList<>();
            NearestOnGrid search = new NearestOnGrid(0, 0, 2500, STEP);
            search.advance(Integer.MAX_VALUE, (chunkX, chunkZ) -> {
                columns.add(chunkX);
                return null;
            });

            List<Integer> sorted = columns.stream().distinct().sorted().toList();
            for (int i = 1; i < sorted.size(); i++) {
                int gap = sorted.get(i) - sorted.get(i - 1);
                assertTrue(gap <= SPACING, "gap of " + gap + " chunks would skip a region");
            }
        }

        @Test
        @DisplayName("probes nearest first, so a sweep cut short still holds the best answer nearby")
        void probesNearestFirst() {
            List<Long> distances = new ArrayList<>();
            NearestOnGrid search = new NearestOnGrid(0, 0, 2500, STEP);
            search.advance(Integer.MAX_VALUE, (chunkX, chunkZ) -> {
                int x = (chunkX << 4) + 8;
                int z = (chunkZ << 4) + 8;
                distances.add((long) x * x + (long) z * z);
                return null;
            });

            for (int i = 1; i < distances.size(); i++) {
                assertTrue(distances.get(i) >= distances.get(i - 1), "sample order is not nearest-first");
            }
        }

        @Test
        @DisplayName("spends only its budget per call")
        void honoursTheBudget() {
            NearestOnGrid search = new NearestOnGrid(0, 0, 2500, STEP);
            int total = search.remaining();

            assertFalse(search.advance(10, EMPTY));
            assertEquals(total - 10, search.remaining());

            while (!search.advance(48, EMPTY)) {
                assertTrue(search.remaining() > 0);
            }
            assertTrue(search.done());
            assertEquals(0, search.remaining());
        }

        @Test
        @DisplayName("keeps a sweep small enough to spread over a handful of ticks")
        void staysAffordable() {
            // At 48 probes a tick this is the whole cost of a locate. If a change here ever pushes
            // it into the thousands, the sweep stops being invisible.
            NearestOnGrid search = new NearestOnGrid(0, 0, 2500, STEP);

            assertNotNull(search);
            assertTrue(search.remaining() < 600, "sweep grew to " + search.remaining() + " probes");
        }
    }
}
