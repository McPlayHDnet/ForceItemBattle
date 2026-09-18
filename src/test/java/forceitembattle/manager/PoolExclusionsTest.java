package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.manager.ItemDifficultiesManager.ItemDefinition;
import forceitembattle.manager.ItemDifficultiesManager.ItemTag;
import forceitembattle.manager.ItemDifficultiesManager.State;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Which items the settings keep out of the generation pool.
 *
 * <p>The subsumption between HARD and EXTREME is the reason these are worth pinning: it is a
 * relationship between two settings, expressed as an {@code if/else if}, that reads like two
 * independent checks. Getting it wrong changes what a whole server can be asked to find, and
 * nothing would throw.
 */
class PoolExclusionsTest {

    private static ItemDefinition item(ItemTag... tags) {
        return new ItemDefinition(Material.STONE, State.EARLY, tags);
    }

    private static final ItemDefinition PLAIN = item();
    private static final ItemDefinition NETHER = item(ItemTag.NETHER);
    private static final ItemDefinition EXTREME = item(ItemTag.EXTREME);
    private static final ItemDefinition END = item(ItemTag.END);

    @Nested
    class EverythingOn {

        @Test
        void keepsEveryKindOfItem() {
            assertFalse(PoolExclusions.isExcluded(PLAIN, true, true, true));
            assertFalse(PoolExclusions.isExcluded(NETHER, true, true, true));
            assertFalse(PoolExclusions.isExcluded(EXTREME, true, true, true));
            assertFalse(PoolExclusions.isExcluded(END, true, true, true));
        }
    }

    @Nested
    class HardOff {

        @Test
        void dropsNetherItems() {
            assertTrue(PoolExclusions.isExcluded(NETHER, false, true, true));
        }

        /**
         * The subsumption: HARD off takes the extreme items with it, so EXTREME being on cannot
         * bring them back. This is the assertion that fails if the {@code else if} ever becomes a
         * second independent {@code if}.
         */
        @Test
        void dropsExtremeItemsEvenWithExtremeOn() {
            assertTrue(PoolExclusions.isExcluded(EXTREME, false, true, true));
        }

        @Test
        void keepsOrdinaryItems() {
            assertFalse(PoolExclusions.isExcluded(PLAIN, false, true, true));
        }
    }

    @Nested
    class HardOnExtremeOff {

        @Test
        void dropsOnlyTheExtremeOnes() {
            assertTrue(PoolExclusions.isExcluded(EXTREME, true, false, true));
            assertFalse(PoolExclusions.isExcluded(NETHER, true, false, true));
            assertFalse(PoolExclusions.isExcluded(PLAIN, true, false, true));
        }
    }

    @Nested
    class EndOff {

        @Test
        void dropsEndItemsIndependentlyOfTheOtherTwo() {
            assertTrue(PoolExclusions.isExcluded(END, true, true, false));
            assertTrue(PoolExclusions.isExcluded(END, false, false, false));
        }

        @Test
        void leavesEverythingElseAlone() {
            assertFalse(PoolExclusions.isExcluded(PLAIN, true, true, false));
            assertFalse(PoolExclusions.isExcluded(NETHER, true, true, false));
        }
    }

    /**
     * A material with no registry entry is not a pool item at all — the custom items ride alongside
     * the pool under vanilla materials, and filtering must not silently drop them.
     */
    @Test
    void anUnregisteredMaterialIsNeverExcluded() {
        assertFalse(PoolExclusions.isExcluded(null, false, false, false));
    }
}
