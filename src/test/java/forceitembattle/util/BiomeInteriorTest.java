package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.util.BiomeInterior.Point;
import forceitembattle.util.BiomeInterior.Probe;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Walking in from a biome's rim.
 *
 * <p>Headless on purpose: the geometry takes a {@link Probe}, so the biome under test is whatever
 * shape this file says it is, and the cases that matter — a ribbon, a blob the walk can step out
 * of, a probe that has stopped working — are all reachable without a server. The real probe is one
 * noise sample and is pinned by {@code BiomeSearch#contains}'s own contract instead.
 */
class BiomeInteriorTest {

    /** An axis-aligned box, the simplest thing with a middle. */
    private static Probe box(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        return (x, y, z) -> x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    @Nested
    @DisplayName("centring")
    class Centring {

        @Test
        @DisplayName("walks from a rim point to the middle of the region")
        void findsTheMiddle() {
            Probe probe = box(-200, 200, -40, -8, -200, 200);

            Point centre = BiomeInterior.centre(new Point(-200, -8, 0), probe);

            assertTrue(Math.abs(centre.x()) <= 16, "x should land near 0, was " + centre.x());
            assertTrue(Math.abs(centre.z()) <= 16, "z should land near 0, was " + centre.z());
            assertTrue(centre.y() < -8, "y should sink below the rim it started on, was " + centre.y());
        }

        @Test
        @DisplayName("moves off the ceiling a rim hit always lands on")
        void sinksAwayFromTheCeiling() {
            // The lattice search orders heights by distance from the player, so a biome search from
            // the surface always returns the shallowest cell of the region. That is the top of the
            // cave biome, where the carvers have had the least to work with.
            Probe probe = box(-100, 100, -50, -10, -100, 100);

            Point centre = BiomeInterior.centre(new Point(0, -10, 0), probe);

            assertEquals(-30, centre.y(), "should sit mid-band, not on the ceiling");
        }

        @Test
        @DisplayName("follows a thin diagonal ribbon rather than leaving it")
        void staysInsideARibbon() {
            // Sulfur caves sit in a weirdness band a fifth as wide as the other cave biomes', so
            // their regions are ribbons. A midpoint computed across one runs straight out of it,
            // which is the failure the re-probe in shift() exists to catch.
            Probe ribbon = (x, y, z) -> Math.abs(x - z) <= 24 && Math.abs(x) <= 400 && y >= -40 && y <= -16;

            Point centre = BiomeInterior.centre(new Point(-400, -16, -400), ribbon);

            assertTrue(ribbon.contains(centre.x(), centre.y(), centre.z()),
                    "walked out of the biome to " + centre);
        }

        @Test
        @DisplayName("keeps the point it was given when the probe rejects it")
        void degradesToTheRimPoint() {
            // The fail-safe. If the point test ever stops working — it rests on two Minecraft
            // internals collapsing a search to a single sample — this must hand back the rim
            // point the locator used before, not a confident guess.
            Point rim = new Point(1234, -30, -567);

            assertEquals(rim, BiomeInterior.centre(rim, (x, y, z) -> false));
        }

        @Test
        @DisplayName("stops probing once it has stopped moving")
        void settles() {
            AtomicInteger probes = new AtomicInteger();
            Probe counted = (x, y, z) -> {
                probes.incrementAndGet();
                return box(-160, 160, -40, -8, -160, 160).contains(x, y, z);
            };

            BiomeInterior.centre(new Point(0, -24, 0), counted);

            // A few hundred noise samples is the whole budget of the free half of the locate; if
            // this ever runs into the thousands the walk has started oscillating.
            assertTrue(probes.get() < 600, "probe count ran away: " + probes.get());
        }
    }
}
