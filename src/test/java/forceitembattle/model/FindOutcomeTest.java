package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The four rules that decide what a find is worth.
 *
 * <p>These used to be inline guards spread through {@code FoundItemListener.onFoundItem}, reachable
 * only by playing a round. Every one of them fails silently when it fails — a find that does not
 * score, or a streak that survives a skip, looks exactly like one that worked — which is why they
 * are pinned individually rather than through the pipeline that applies them.
 */
class FindOutcomeTest {

    private static final long NOW = 100_000L;

    private static GameContext context(boolean runMode, boolean statsEnabled) {
        return new GameContext(false, runMode, false, statsEnabled, false);
    }

    private static ForceItemPlayer finder(long assignedAt, int itemStreak) {
        ForceItemPlayer player = new ForceItemPlayer(Players.mockPlayer("a"), Material.DIRT, 3, 0);
        player.setLastItemAssignedAt(assignedAt);
        player.setItemStreak(itemStreak);
        return player;
    }

    private static Find find(boolean skipped, boolean backToBack) {
        return new Find(finder(NOW - 5_000L, 4), Material.STONE, skipped, backToBack);
    }

    private static FindOutcome outcome(Find find, GameContext context) {
        return FindOutcome.of(find, context, NOW);
    }

    @Nested
    class Announcing {

        @Test
        void anOrdinaryFindIsAnnounced() {
            assertTrue(outcome(find(false, false), context(false, true)).announces());
        }

        /** BackToBackManager announces these itself, with the odds attached. */
        @Test
        void aBackToBackIsNotAnnouncedHere() {
            assertFalse(outcome(find(false, true), context(false, true)).announces());
        }
    }

    @Nested
    class Scoring {

        @Test
        void aSkipStillScoresOutsideRunMode() {
            assertTrue(outcome(find(true, false), context(false, true)).scores());
        }

        /** Run mode is a race for the first find, so buying your way past it earns nothing. */
        @Test
        void aSkipDoesNotScoreInRunMode() {
            assertFalse(outcome(find(true, false), context(true, true)).scores());
        }

        @Test
        void anActualFindScoresInEitherMode() {
            assertTrue(outcome(find(false, false), context(false, true)).scores());
            assertTrue(outcome(find(false, false), context(true, true)).scores());
        }
    }

    @Nested
    class StatsGating {

        @Test
        void statsAreRecordedWhenEnabledOutsideRunMode() {
            assertTrue(outcome(find(false, false), context(false, true)).recordsStats());
        }

        @Test
        void runModeRecordsNothingEvenWithStatsOn() {
            assertFalse(outcome(find(false, false), context(true, true)).recordsStats());
        }

        @Test
        void statsOffRecordsNothing() {
            assertFalse(outcome(find(false, false), context(false, false)).recordsStats());
        }
    }

    @Nested
    class TimeSpent {

        @Test
        void isMeasuredFromWhenTheItemWasHandedOut() {
            assertEquals(5_000L, outcome(find(false, false), context(false, true)).timeSpentMs());
        }

        /** A back-to-back was never hunted, so it took no time. */
        @Test
        void isZeroForABackToBack() {
            assertEquals(0L, outcome(find(false, true), context(false, true)).timeSpentMs());
        }

        /**
         * An item that was never formally handed out has no stamp to measure from, and a duration
         * counted from the epoch would be reported as roughly 56 years on the item.
         */
        @Test
        void isZeroWhenTheItemWasNeverStamped() {
            Find unstamped = new Find(finder(0L, 0), Material.STONE, false, false);

            assertEquals(0L, outcome(unstamped, context(false, true)).timeSpentMs());
        }
    }

    @Nested
    class ItemStreak {

        @Test
        void anObtainedItemExtendsIt() {
            assertEquals(5, outcome(find(false, false), context(false, true)).newItemStreak());
        }

        /** Back-to-backs count: the streak is every item obtained, however it arrived. */
        @Test
        void aBackToBackExtendsIt() {
            assertEquals(5, outcome(find(false, true), context(false, true)).newItemStreak());
        }

        @Test
        void aSkipBreaksIt() {
            assertEquals(0, outcome(find(true, false), context(false, true)).newItemStreak());
        }
    }
}
