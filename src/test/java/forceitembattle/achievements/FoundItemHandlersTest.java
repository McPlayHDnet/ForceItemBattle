package forceitembattle.achievements;

import static forceitembattle.achievements.Finds.backToBack;
import static forceitembattle.achievements.Finds.found;
import static forceitembattle.achievements.Finds.participant;
import static forceitembattle.achievements.Finds.skipped;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.achievements.handlers.BackToBackCountAchievementHandler;
import forceitembattle.achievements.handlers.ConsecutiveStoneAchievementHandler;
import forceitembattle.achievements.handlers.CounterAchievementHandler;
import forceitembattle.achievements.handlers.NoBackToBackAchievementHandler;
import forceitembattle.achievements.handlers.RepeatItemAchievementHandler;
import forceitembattle.achievements.handlers.SameItemBackToBackAchievementHandler;
import forceitembattle.achievements.handlers.SkipAchievementHandler;
import forceitembattle.achievements.progress.ConsecutiveStoneAchievementProgress;
import forceitembattle.achievements.progress.CounterAchievementProgress;
import forceitembattle.achievements.progress.ItemFrequencyAchievementProgress;
import forceitembattle.achievements.progress.SameItemBackToBackAchievementProgress;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.achievements.progress.SkipAchievementProgress;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The achievement rules driven by a find.
 *
 * <p>These are counters and streaks over a progress tracker — pure arithmetic, and the reason the
 * seam was worth cutting. Until {@code check} stopped taking a {@code ForceItemBattle}, exercising
 * any of them meant standing up 23 managers, so none of them was tested and the package's rules
 * were only ever verified by playing.
 *
 * <p>What is pinned here is mostly <em>what breaks a streak</em>. Every one of these handlers has a
 * different answer, none of them was written down, and the differences are invisible at the call
 * site because the manager drives them all through the same interface.
 */
class FoundItemHandlersTest {

    @Nested
    class BackToBackCount {

        @Test
        void countsOnlyBackToBacks() {
            ForceItemPlayer alice = participant("a");
            BackToBackCountAchievementHandler handler = new BackToBackCountAchievementHandler(2);
            SimpleAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(backToBack(alice, Material.STONE), progress, alice, world));
            assertTrue(handler.check(backToBack(alice, Material.OAK_LOG), progress, alice, world));
        }

        /** A plain find does not reset the tally: this counts back-to-backs, it does not chain them. */
        @Test
        void anOrdinaryFindDoesNotResetTheTally() {
            ForceItemPlayer alice = participant("a");
            BackToBackCountAchievementHandler handler = new BackToBackCountAchievementHandler(2);
            SimpleAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world));
            assertTrue(handler.check(backToBack(alice, Material.OAK_LOG), progress, alice, world),
                    "the interruption is not a reset here, unlike the consecutive handlers");
        }
    }

    @Nested
    class ConsecutiveStone {

        @Test
        void countsAnUnbrokenRunOfStoneTypes() {
            ForceItemPlayer alice = participant("a");
            ConsecutiveStoneAchievementHandler handler = new ConsecutiveStoneAchievementHandler(2);
            ConsecutiveStoneAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world));
            assertTrue(handler.check(found(alice, Material.COBBLESTONE), progress, alice, world));
        }

        @Test
        void aNonStoneItemBreaksTheRun() {
            ForceItemPlayer alice = participant("a");
            ConsecutiveStoneAchievementHandler handler = new ConsecutiveStoneAchievementHandler(2);
            ConsecutiveStoneAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.COBBLESTONE), progress, alice, world),
                    "the run restarted, so one stone is not yet two");
        }

        /** A skip breaks the run without even looking at what was skipped. */
        @Test
        void aSkipBreaksTheRun() {
            ForceItemPlayer alice = participant("a");
            ConsecutiveStoneAchievementHandler handler = new ConsecutiveStoneAchievementHandler(2);
            ConsecutiveStoneAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world));
            assertFalse(handler.check(skipped(alice, Material.STONE), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.COBBLESTONE), progress, alice, world));
        }
    }

    @Nested
    class RepeatItem {

        @Test
        void countsPerMaterialRatherThanInTotal() {
            ForceItemPlayer alice = participant("a");
            RepeatItemAchievementHandler handler = new RepeatItemAchievementHandler(2);
            ItemFrequencyAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world),
                    "a different material has its own tally");
            assertTrue(handler.check(found(alice, Material.DIRT), progress, alice, world));
        }

        /** No skip check here — being handed the same item twice counts however it arrived. */
        @Test
        void aSkippedItemStillCounts() {
            ForceItemPlayer alice = participant("a");
            RepeatItemAchievementHandler handler = new RepeatItemAchievementHandler(2);
            ItemFrequencyAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(skipped(alice, Material.DIRT), progress, alice, world));
            assertTrue(handler.check(found(alice, Material.DIRT), progress, alice, world));
        }
    }

    @Nested
    class SameItemBackToBack {

        @Test
        void needsTheSameMaterialBackToBackTwice() {
            ForceItemPlayer alice = participant("a");
            SameItemBackToBackAchievementHandler handler = new SameItemBackToBackAchievementHandler(2);
            SameItemBackToBackAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
            assertTrue(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
        }

        @Test
        void aBackToBackOnAnotherMaterialStartsAFreshRun() {
            ForceItemPlayer alice = participant("a");
            SameItemBackToBackAchievementHandler handler = new SameItemBackToBackAchievementHandler(2);
            SameItemBackToBackAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(backToBack(alice, Material.STONE), progress, alice, world));
            assertTrue(handler.check(backToBack(alice, Material.STONE), progress, alice, world));
        }

        /** An ordinary find breaks the chain even on the same material. */
        @Test
        void anOrdinaryFindBreaksTheChain() {
            ForceItemPlayer alice = participant("a");
            SameItemBackToBackAchievementHandler handler = new SameItemBackToBackAchievementHandler(2);
            SameItemBackToBackAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
        }
    }

    @Nested
    class Counter {

        @Test
        void aPlainCounterCountsEveryFind() {
            ForceItemPlayer alice = participant("a");
            CounterAchievementHandler handler = new CounterAchievementHandler(2, false, null);
            CounterAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, world));
            assertTrue(handler.check(found(alice, Material.STONE), progress, alice, world));
        }

        /**
         * The one handler that asks the world anything: a dimension-scoped counter only counts
         * items the pool can hand out in that dimension.
         */
        @Test
        void aDimensionCounterOnlyCountsThatDimensionsItems() {
            ForceItemPlayer alice = participant("a");
            CounterAchievementHandler handler =
                    new CounterAchievementHandler(1, false, Dimension.NETHER);
            FakeAchievementWorld world = new FakeAchievementWorld()
                    .itemsIn(Dimension.NETHER, Material.NETHERRACK);

            assertFalse(handler.check(found(alice, Material.DIRT), handler.createProgress(), alice, world),
                    "an overworld item does not count toward a nether achievement");
            assertTrue(handler.check(found(alice, Material.NETHERRACK), handler.createProgress(), alice, world));
        }

        @Test
        void aConsecutiveCounterIsResetByASkip() {
            ForceItemPlayer alice = participant("a");
            CounterAchievementHandler handler = new CounterAchievementHandler(2, true, null);
            CounterAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(found(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(skipped(alice, Material.STONE), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.OAK_LOG), progress, alice, world));
        }
    }

    @Nested
    class Skips {

        @Test
        void countsSkipsToTheTarget() {
            ForceItemPlayer alice = participant("a");
            SkipAchievementHandler handler = new SkipAchievementHandler(2, false, 0);
            SkipAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5400);

            assertFalse(handler.check(skipped(alice, Material.DIRT), progress, alice, world));
            assertTrue(handler.check(skipped(alice, Material.STONE), progress, alice, world));
        }

        @Test
        void aFindBreaksAConsecutiveSkipRun() {
            ForceItemPlayer alice = participant("a");
            SkipAchievementHandler handler = new SkipAchievementHandler(2, true, 0);
            SkipAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5400);

            assertFalse(handler.check(skipped(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(found(alice, Material.STONE), progress, alice, world));
            assertFalse(handler.check(skipped(alice, Material.OAK_LOG), progress, alice, world));
        }

        /**
         * A windowed skip counts only if the item was held for less than the window. The clock is
         * the world's, so this is assertable by moving a number rather than by waiting.
         */
        @Test
        void aWindowedSkipCountsOnlyInsideItsWindow() {
            ForceItemPlayer alice = participant("a");
            SkipAchievementHandler handler = new SkipAchievementHandler(1, false, 10);
            SkipAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5400);

            // First event anchors the marker and cannot itself count.
            assertFalse(handler.check(skipped(alice, Material.DIRT), progress, alice, world));

            world.secondsLeft(5395);
            assertTrue(handler.check(skipped(alice, Material.STONE), progress, alice, world),
                    "five seconds after the last item, inside a ten-second window");
        }

        @Test
        void aWindowedSkipOutsideItsWindowDoesNotCount() {
            ForceItemPlayer alice = participant("a");
            SkipAchievementHandler handler = new SkipAchievementHandler(1, false, 10);
            SkipAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld().clock(5400, 5400);

            assertFalse(handler.check(skipped(alice, Material.DIRT), progress, alice, world));

            world.secondsLeft(5380);
            assertFalse(handler.check(skipped(alice, Material.STONE), progress, alice, world),
                    "twenty seconds is outside a ten-second window");
        }
    }

    /**
     * Two handlers never grant anything mid-round — they only accumulate, and the game-end pass
     * reads the tally. A test is the only way to see that, because returning false forever looks
     * identical to being broken.
     */
    @Nested
    class EvaluatedAtGameEnd {

        @Test
        void noBackToBackAccumulatesButNeverGrants() {
            ForceItemPlayer alice = participant("a");
            NoBackToBackAchievementHandler handler = new NoBackToBackAchievementHandler();
            SimpleAchievementProgress progress = handler.createProgress();
            FakeAchievementWorld world = new FakeAchievementWorld();

            assertFalse(handler.check(backToBack(alice, Material.DIRT), progress, alice, world));
            assertFalse(handler.check(backToBack(alice, Material.STONE), progress, alice, world));
            assertTrue(progress.count >= 2, "the tally is what the game-end pass reads");
        }
    }
}
