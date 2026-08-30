package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.settings.QuickieMode;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * When the item pools open.
 *
 * <p>The schedule was previously reachable only through a manager carrying 1,382 lines of item
 * data, which needed a mocked plugin, settings, timer and game manager to answer "is MID open
 * yet?". None of those are properties of the question, and none appear here.
 */
class UnlockScheduleTest {

    private static int minutes(int m) {
        return m * 60;
    }

    @Nested
    class ShortRounds {

        /** Under 50 minutes the pools are spread across the round by percentage. */
        @Test
        void spreadThePoolsAcrossTheRound() {
            UnlockSchedule schedule = UnlockSchedule.forRound(45);

            assertEquals(0, schedule.unlockMinute(State.EARLY, minutes(45)));
            assertEquals(5, schedule.unlockMinute(State.MID, minutes(45)));
            assertEquals(13, schedule.unlockMinute(State.LATE, minutes(45)));
        }

        @Test
        void theThresholdItselfIsAlreadyALongRound() {
            UnlockSchedule justUnder = UnlockSchedule.forRound(UnlockSchedule.FIXED_SCHEDULE_MIN_MINUTES - 1);
            UnlockSchedule atThreshold = UnlockSchedule.forRound(UnlockSchedule.FIXED_SCHEDULE_MIN_MINUTES);

            assertEquals(14, justUnder.unlockMinute(State.LATE, minutes(49)));
            assertEquals(15, atThreshold.unlockMinute(State.LATE, minutes(50)));
        }
    }

    @Nested
    class LongRounds {

        /**
         * The reason the fixed schedule exists, stated as a comparison: on a 90-minute game the
         * percentage schedule holds LATE back for 26 minutes.
         */
        @Test
        void useFixedMarksInsteadOfHoldingLateBack() {
            assertEquals(26, UnlockSchedule.percentageBased().unlockMinute(State.LATE, minutes(90)));
            assertEquals(15, UnlockSchedule.forRound(90).unlockMinute(State.LATE, minutes(90)));
        }

        @Test
        void putMidAtFiveMinutesWhateverTheLength() {
            assertEquals(5, UnlockSchedule.forRound(60).unlockMinute(State.MID, minutes(60)));
            assertEquals(5, UnlockSchedule.forRound(120).unlockMinute(State.MID, minutes(120)));
        }
    }

    @Nested
    class WhatIsOpen {

        private final UnlockSchedule schedule = UnlockSchedule.forRound(45);
        private final int round = minutes(45);

        @Test
        void onlyEarlyAtTheStart() {
            assertEquals(List.of(State.EARLY), schedule.activeAt(0, round, QuickieMode.DISABLED));
        }

        @Test
        void aPoolIsOpenOnTheMinuteItUnlocks() {
            assertEquals(List.of(State.EARLY), schedule.activeAt(4, round, QuickieMode.DISABLED));
            assertEquals(List.of(State.EARLY, State.MID), schedule.activeAt(5, round, QuickieMode.DISABLED));
        }

        @Test
        void everythingIsOpenLateOn() {
            assertEquals(List.of(State.EARLY, State.MID, State.LATE),
                    schedule.activeAt(40, round, QuickieMode.DISABLED));
        }

        @Test
        void quickieModeCapsWhatCanOpenAtAll() {
            assertEquals(List.of(State.EARLY), schedule.activeAt(40, round, QuickieMode.EARLY));
            assertEquals(List.of(State.EARLY, State.MID), schedule.activeAt(40, round, QuickieMode.EARLY_MID));
        }
    }

    @Nested
    class WhatIsNext {

        private final UnlockSchedule schedule = UnlockSchedule.forRound(45);
        private final int round = minutes(45);

        @Test
        void namesThePoolThatHasNotOpenedYet() {
            assertEquals(State.MID, schedule.nextAfter(0, round, QuickieMode.DISABLED));
            assertEquals(State.LATE, schedule.nextAfter(5, round, QuickieMode.DISABLED));
        }

        @Test
        void thereIsNothingNextOnceEverythingIsOpen() {
            assertNull(schedule.nextAfter(40, round, QuickieMode.DISABLED));
        }

        /** A capping quickie mode looks the same as "everything is already open". */
        @Test
        void thereIsNothingNextWhenQuickieModeCapsThePools() {
            assertNull(schedule.nextAfter(0, round, QuickieMode.EARLY));
        }

        @Test
        void countsDownToTheNextUnlock() {
            assertEquals(minutes(5), schedule.secondsUntilNext(0, round, QuickieMode.DISABLED));
            assertEquals(60, schedule.secondsUntilNext(minutes(4), round, QuickieMode.DISABLED));
        }

        /**
         * On the tick a pool opens it stops being "next", so the countdown hands over to the one
         * after it rather than sitting at zero. MID opens at 5 minutes: a second before, the
         * countdown reads 1; on the minute itself it is already counting down to LATE at 13.
         */
        @Test
        void theCountdownHandsOverOnTheTickThePoolOpens() {
            assertEquals(1, schedule.secondsUntilNext(minutes(5) - 1, round, QuickieMode.DISABLED));
            assertEquals(minutes(13) - minutes(5),
                    schedule.secondsUntilNext(minutes(5), round, QuickieMode.DISABLED));
        }

        @Test
        void countdownIsMinusOneWhenNothingRemains() {
            assertEquals(-1, schedule.secondsUntilNext(minutes(40), round, QuickieMode.DISABLED));
        }
    }
}
