package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link Landing}: whether a scatter destination needs a floor putting under it.
 *
 * <p>{@link TheGuardThatWasThere} pins the two mistakes available here, so neither can be
 * reintroduced by someone reaching for the predicate that reads best.
 */
class LandingTest {

    @Nested
    class NeedsAFloor {

        /** The case the rule exists for: an empty column, so nothing beneath the destination. */
        @ParameterizedTest
        @EnumSource(value = Material.class, names = {"AIR", "CAVE_AIR", "VOID_AIR"})
        void anEmptyColumnGetsOne(Material below) {
            assertTrue(Landing.needsFloor(below), below + " leaves nothing to stand on");
        }
    }

    @Nested
    class DoesNot {

        @ParameterizedTest
        @EnumSource(value = Material.class, names = {"STONE", "GRASS_BLOCK", "DEEPSLATE", "SAND"})
        void ordinaryGroundIsLeftAlone(Material below) {
            assertFalse(Landing.needsFloor(below));
        }

        /**
         * Landing in an ocean is a landing. Plugging it with stone would be worse than the swim,
         * and it is what the obvious-looking alternative predicate would do.
         */
        @ParameterizedTest
        @EnumSource(value = Material.class, names = {"WATER", "LAVA"})
        void liquidIsNotPluggedWithStone(Material below) {
            assertFalse(Landing.needsFloor(below), below + " is somewhere you land, not a hole");
        }

        /** Ground cover is ground: a snowy plain or a meadow is the top block in its column. */
        @ParameterizedTest
        @EnumSource(value = Material.class, names = {"SNOW", "SHORT_GRASS", "TALL_GRASS", "FERN"})
        void groundCoverIsNotScarred(Material below) {
            assertFalse(Landing.needsFloor(below), below + " would get a stone block through it");
        }
    }

    /** The two ways to get this wrong. Both predicates read like the right question and neither is. */
    @Nested
    class TheGuardThatWasThere {

        /**
         * {@code isBlock()} is true for air, so a guard built on it is false for every material a
         * player can land on and the floor is never placed.
         */
        @Test
        void isBlockCannotExpressThisAtAll() {
            assertTrue(Material.AIR.isBlock(), "which is why such a guard never fires");
            assertTrue(Material.WATER.isBlock());
            assertTrue(Material.STONE.isBlock());
        }

        /**
         * The tempting over-correction: {@code isSolid()} is false for water, lava and ground cover,
         * so swapping it in would plug oceans and scar meadows.
         */
        @Test
        void isSolidWouldFireOnPlacesThatAreFineToLandOn() {
            assertFalse(Material.WATER.isSolid());
            assertFalse(Material.SNOW.isSolid());
            assertFalse(Material.SHORT_GRASS.isSolid());

            assertFalse(Landing.needsFloor(Material.WATER), "so the rule does not use isSolid");
            assertFalse(Landing.needsFloor(Material.SNOW));
            assertFalse(Landing.needsFloor(Material.SHORT_GRASS));
        }
    }
}
