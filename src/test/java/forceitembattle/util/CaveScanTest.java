package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Reading a dig spot out of generated chunks.
 *
 * <p>The worlds here are written by hand, three lines each, which is the point of {@code CaveScan}
 * taking snapshots: the cases worth pinning are all "what is in this column", and none of them
 * need a server to state. {@link Biome} is mocked rather than named — the scan only ever compares
 * it by identity, and the real constants want a registry.
 */
class CaveScanTest {

    private static final Biome SULFUR_CAVES = mock(Biome.class);
    private static final Biome ELSEWHERE = mock(Biome.class);

    private static final int MIN_HEIGHT = -64;
    private static final int MAX_HEIGHT = 320;

    /** A column of the little world a test describes. */
    @FunctionalInterface
    private interface Blocks {
        Material at(int worldX, int y, int worldZ);
    }

    /** Which biome a point belongs to, when a test cares. */
    @FunctionalInterface
    private interface Biomes {
        Biome at(int worldX, int y, int worldZ);
    }

    private static ChunkSnapshot chunk(int chunkX, int chunkZ, Blocks blocks) {
        return chunk(chunkX, chunkZ, blocks, (x, y, z) -> SULFUR_CAVES);
    }

    private static ChunkSnapshot chunk(int chunkX, int chunkZ, Blocks blocks, Biomes biomes) {
        ChunkSnapshot snapshot = mock(ChunkSnapshot.class);

        when(snapshot.getX()).thenReturn(chunkX);
        when(snapshot.getZ()).thenReturn(chunkZ);
        when(snapshot.contains(any(Biome.class))).thenReturn(true);

        when(snapshot.getBlockType(anyInt(), anyInt(), anyInt())).thenAnswer(call ->
                blocks.at((chunkX << 4) + (int) call.getArgument(0),
                        call.getArgument(1),
                        (chunkZ << 4) + (int) call.getArgument(2)));

        when(snapshot.getBiome(anyInt(), anyInt(), anyInt())).thenAnswer(call ->
                biomes.at((chunkX << 4) + (int) call.getArgument(0),
                        call.getArgument(1),
                        (chunkZ << 4) + (int) call.getArgument(2)));

        when(snapshot.getHighestBlockYAt(anyInt(), anyInt())).thenAnswer(call -> {
            int worldX = (chunkX << 4) + (int) call.getArgument(0);
            int worldZ = (chunkZ << 4) + (int) call.getArgument(1);
            for (int y = MAX_HEIGHT - 1; y > MIN_HEIGHT; y--) {
                Material material = blocks.at(worldX, y, worldZ);
                if (material != Material.AIR && material != Material.CAVE_AIR) {
                    return y;
                }
            }
            return MIN_HEIGHT;
        });

        return snapshot;
    }

    private static CaveScan.Target scan(List<ChunkSnapshot> chunks, int centreY) {
        return CaveScan.scan(chunks, SULFUR_CAVES, 8, centreY, 8, MIN_HEIGHT, MAX_HEIGHT);
    }

    /** Stone from bedrock to y=64, sky above. */
    private static Material solidGround(int y) {
        return y <= 64 ? Material.STONE : Material.AIR;
    }

    @Nested
    @DisplayName("sulfur at the surface")
    class SurfaceSulfur {

        @Test
        @DisplayName("finds sulfur sitting in the top of a column")
        void findsSurfaceSulfur() {
            CaveScan.Target target = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 5 && z == 6 && y == 64) {
                    return Material.POTENT_SULFUR;
                }
                return solidGround(y);
            })), -20);

            assertNotNull(target);
            assertEquals(CaveScan.Find.SURFACE, target.find());
            assertEquals(5, target.x());
            assertEquals(64, target.y());
            assertEquals(6, target.z());
        }

        @Test
        @DisplayName("prefers surface sulfur over any cave pocket")
        void surfaceBeatsCave() {
            // Sulfur at the surface is either a spring wired to the cave by its own column or the
            // cave cut open, and both are easier to reach than a hole that has to be dug down to.
            CaveScan.Target target = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 9 && z == 9 && y == 64) {
                    return Material.SULFUR_SPIKE;
                }
                if (x == 8 && z == 8 && y >= -22 && y <= -16) {
                    return Material.CAVE_AIR;
                }
                return solidGround(y);
            })), -20);

            assertNotNull(target);
            assertEquals(CaveScan.Find.SURFACE, target.find());
        }

        @Test
        @DisplayName("counts the cave cut open at the surface, not just springs")
        void countsAnExposedCave() {
            // The case that made the old wording a lie. The overworld surface rules paint sulfur
            // and cinnabar as the walls of a sulfur cave, so a ravine through the biome tops a
            // column with them and no spring is involved. It is still exactly where to dig — the
            // find says SURFACE and the message no longer claims a spring.
            CaveScan.Target target = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 7 && z == 7) {
                    return y == 64 ? Material.CINNABAR : (y > 20 && y < 64 ? Material.CAVE_AIR : solidGround(y));
                }
                return solidGround(y);
            })), -20);

            assertNotNull(target);
            assertEquals(CaveScan.Find.SURFACE, target.find());
        }

        @Test
        @DisplayName("ignores sulfur buried far below the surface")
        void ignoresBuriedSulfur() {
            // Sulfur, spikes and cinnabar are all over the cave itself — the surface rules paint
            // its walls with them. Only their reaching the top of a column is a signal.
            CaveScan.Target target = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (y == -20) {
                    return Material.SULFUR;
                }
                return solidGround(y);
            })), -20);

            assertNull(target);
        }
    }

    @Nested
    @DisplayName("caves")
    class Caves {

        @Test
        @DisplayName("falls back to the floor of a cave pocket")
        void findsThePocketFloor() {
            CaveScan.Target target = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 8 && z == 8 && y >= -22 && y <= -16) {
                    return Material.CAVE_AIR;
                }
                return solidGround(y);
            })), -20);

            assertNotNull(target);
            assertEquals(CaveScan.Find.CAVE, target.find());
            assertEquals(-22, target.y(), "should land on the floor of the pocket, not in its middle");
        }

        @Test
        @DisplayName("counts cave air, which is what carvers actually leave")
        void caveAirCounts() {
            // Air and cave air are different materials, and every carved cave in the world is the
            // second one. A scan that only knew about AIR would quietly find nothing, forever.
            CaveScan.Target airPocket = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 8 && z == 8 && y >= -22 && y <= -16) {
                    return Material.AIR;
                }
                return solidGround(y);
            })), -20);
            CaveScan.Target caveAirPocket = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 8 && z == 8 && y >= -22 && y <= -16) {
                    return Material.CAVE_AIR;
                }
                return solidGround(y);
            })), -20);

            assertNotNull(airPocket);
            assertNotNull(caveAirPocket);
            assertEquals(airPocket.y(), caveAirPocket.y());
        }

        @Test
        @DisplayName("skips a crack too short to stand in")
        void skipsCracks() {
            CaveScan.Target target = scan(List.of(chunk(0, 0, (x, y, z) -> {
                if (x == 8 && z == 8 && y >= -21 && y <= -20) {
                    return Material.CAVE_AIR;
                }
                return solidGround(y);
            })), -20);

            assertNull(target);
        }

        @Test
        @DisplayName("skips air that is not in the biome we were sent for")
        void skipsForeignAir() {
            // The window around the biome's middle catches ordinary caves next door. Digging a
            // player down to one of those is exactly the trip this locator is supposed to stop.
            CaveScan.Target target = scan(List.of(chunk(0, 0,
                    (x, y, z) -> {
                        if (x == 8 && z == 8 && y >= -22 && y <= -16) {
                            return Material.CAVE_AIR;
                        }
                        return solidGround(y);
                    },
                    (x, y, z) -> ELSEWHERE)), -20);

            assertNull(target);
        }

        @Test
        @DisplayName("prefers the pocket nearest the point already being pointed at")
        void prefersTheNearerPocket() {
            // The boss bar has been aiming somewhere for as long as the chunks took to generate,
            // and a target that jumps at the end reads as a bug even when it is a better hole.
            CaveScan.Target target = scan(List.of(
                    chunk(0, 0, (x, y, z) -> {
                        if (x == 10 && z == 10 && y >= -22 && y <= -16) {
                            return Material.CAVE_AIR;
                        }
                        return solidGround(y);
                    }),
                    chunk(2, 2, (x, y, z) -> {
                        if (x == 40 && z == 40 && y >= -30 && y <= -10) {
                            return Material.CAVE_AIR;
                        }
                        return solidGround(y);
                    })), -20);

            assertNotNull(target);
            assertEquals(10, target.x());
            assertEquals(10, target.z());
        }
    }
}
