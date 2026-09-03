package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Where a scatter sends a player, and where it has sent them before.
 *
 * <p>None of this was reachable while it lived in {@code PortalListener}: the maps were private
 * fields of a Bukkit listener and the draw was a private method beside them. The two rules that can
 * be wrong — "the same pad sends you to the same place" and "a different pad does not" — had no test
 * and are one comparison apart.
 *
 * <p>A mocked {@link World} is enough here. {@link Location} arithmetic and {@code distanceSquared}
 * only need the two worlds to be the same object; nothing below asks the world anything.
 */
class ScatterDestinationsTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private World world;
    private ScatterDestinations destinations;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        destinations = new ScatterDestinations(new Random(1234));
    }

    private Location at(double x, double z) {
        return new Location(world, x, 64, z);
    }

    @Nested
    @DisplayName("the antimatter teleporter")
    class Teleporter {

        @Test
        void anUnusedPadHasNoDestinationYet() {
            assertTrue(destinations.existingTeleporterDestination(ALICE, at(0, 0)).isEmpty());
        }

        @Test
        void theSamePadTwiceSendsYouToTheSamePlace() {
            Location destination = at(9_000, 9_000);
            destinations.rememberTeleporter(ALICE, at(0, 0), destination);

            assertSame(destination,
                    destinations.existingTeleporterDestination(ALICE, at(0, 0)).orElseThrow());
        }

        /** Within 25 blocks is the same pad: you never stand on exactly the block you did before. */
        @Test
        void steppingBackOntoThePadFromAFewBlocksAwayStillCounts() {
            Location destination = at(9_000, 9_000);
            destinations.rememberTeleporter(ALICE, at(0, 0), destination);

            assertTrue(destinations.existingTeleporterDestination(ALICE, at(20, 0)).isPresent());
        }

        /** Past 25 blocks it is a different pad, and gets a destination of its own. */
        @Test
        void aDifferentPadIsADifferentPlace() {
            destinations.rememberTeleporter(ALICE, at(0, 0), at(9_000, 9_000));

            assertTrue(destinations.existingTeleporterDestination(ALICE, at(200, 0)).isEmpty());
        }

        /** The memory is per player: a pad someone else has used is new to you. */
        @Test
        void anotherPlayersPadIsNotYours() {
            destinations.rememberTeleporter(ALICE, at(0, 0), at(9_000, 9_000));

            assertTrue(destinations.existingTeleporterDestination(BOB, at(0, 0)).isEmpty());
        }

        /** Two pads, remembered separately, each keep their own destination. */
        @Test
        void twoPadsKeepTwoDestinations() {
            Location first = at(9_000, 9_000);
            Location second = at(-9_000, -9_000);
            destinations.rememberTeleporter(ALICE, at(0, 0), first);
            destinations.rememberTeleporter(ALICE, at(500, 500), second);

            assertSame(first, destinations.existingTeleporterDestination(ALICE, at(0, 0)).orElseThrow());
            assertSame(second, destinations.existingTeleporterDestination(ALICE, at(500, 500)).orElseThrow());
        }

        /**
         * Reading must not write. The lookup used to be a {@code computeIfAbsent} whose only effect
         * was to create the list the write then assumed existed — an ordering rule nothing enforced.
         */
        @Test
        void askingWhereYouHaveBeenDoesNotRecordAnything() {
            destinations.existingTeleporterDestination(ALICE, at(0, 0));
            destinations.rememberTeleporter(ALICE, at(0, 0), at(9_000, 9_000));

            assertTrue(destinations.existingTeleporterDestination(ALICE, at(0, 0)).isPresent());
        }
    }

    @Nested
    @DisplayName("the End")
    class TheEnd {

        /** There is one End, so it is remembered per player rather than per portal. */
        @Test
        void theEndIsRememberedForThePlayerNotThePortal() {
            Location island = at(9_000, 9_000);
            destinations.rememberEnd(ALICE, island);

            assertSame(island, destinations.existingEndDestination(ALICE).orElseThrow());
        }

        @Test
        void theEndIsRememberedIndependentlyOfTheTeleporter() {
            destinations.rememberTeleporter(ALICE, at(0, 0), at(1_000, 1_000));

            assertEquals(Optional.empty(), destinations.existingEndDestination(ALICE));
        }

        @Test
        void anotherPlayersIslandIsNotYours() {
            destinations.rememberEnd(ALICE, at(9_000, 9_000));

            assertTrue(destinations.existingEndDestination(BOB).isEmpty());
        }
    }

    @Nested
    @DisplayName("the draw")
    class Draw {

        /** 5000..15000 blocks on each axis, either sign — the range both scatters have always used. */
        @RepeatedTest(50)
        void everyAxisLandsInRange() {
            ScatterDestinations unseeded = new ScatterDestinations();
            Location target = unseeded.scatterTargetFrom(at(0, 0));

            assertInRange(target.getX());
            assertInRange(target.getZ());
        }

        private static void assertInRange(double offset) {
            double magnitude = Math.abs(offset);
            assertTrue(magnitude >= 5_000 && magnitude <= 15_000,
                    "expected 5000..15000 blocks out, got " + offset);
        }

        /** The target is relative to where the player stands, not to the origin of the world. */
        @Test
        void theDrawIsRelativeToWhereYouStand() {
            Location target = destinations.scatterTargetFrom(at(1_000_000, 0));

            assertTrue(target.getX() - 1_000_000 != target.getX());
            assertInRange(target.getX() - 1_000_000);
        }

        /** Height is left alone: grounding it on the highest block is the listener's job. */
        @Test
        void theHeightIsTheOriginsAndTheWorldIsTheOriginsToo() {
            Location target = destinations.scatterTargetFrom(at(0, 0));

            assertEquals(64, target.getY());
            assertSame(world, target.getWorld());
        }

        /** Two draws are not the same draw, or a pad would be a free trip home. */
        @Test
        void twoDrawsDiffer() {
            assertNotEquals(destinations.scatterTargetFrom(at(0, 0)).getX(),
                    destinations.scatterTargetFrom(at(0, 0)).getX());
        }
    }
}
