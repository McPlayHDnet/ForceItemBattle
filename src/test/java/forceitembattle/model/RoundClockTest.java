package forceitembattle.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The round clock, run forward without a server: a full round is a loop. */
class RoundClockTest {

    private static RoundClock clockAt(int secondsLeft) {
        RoundClock clock = new RoundClock();
        clock.setSecondsLeft(secondsLeft);
        return clock;
    }

    /** Every milestone announced while running from {@code from} down to zero. */
    private static List<Integer> milestonesFrom(int from) {
        RoundClock clock = clockAt(from);
        List<Integer> announced = new ArrayList<>();

        while (!clock.expired()) {
            clock.tick().ifPresent(announced::add);
        }
        return announced;
    }

    @Nested
    class Counting {

        @Test
        void tickingTakesOneSecondOff() {
            RoundClock clock = clockAt(90);

            clock.tick();

            assertEquals(89, clock.secondsLeft());
        }

        @Test
        void aFreshClockHasNotExpiredUntilItReachesZero() {
            RoundClock clock = clockAt(2);

            assertFalse(clock.expired());
            clock.tick();
            assertFalse(clock.expired());
            clock.tick();
            assertTrue(clock.expired());
        }

        /**
         * A round configured with no time is already over, and must not have to be ticked below
         * zero before anything notices.
         */
        @Test
        void aZeroClockIsExpiredImmediately() {
            assertTrue(clockAt(0).expired());
        }
    }

    @Nested
    class Milestones {

        @Test
        void aFullSixtyMinuteRoundAnnouncesExactlyTheNine() {
            assertEquals(List.of(300, 60, 30, 10, 5, 4, 3, 2, 1), milestonesFrom(60 * 60));
        }

        /** Starting below a milestone must not resurrect it. */
        @Test
        void aShortRoundOnlyAnnouncesWhatItReaches() {
            assertEquals(List.of(30, 10, 5, 4, 3, 2, 1), milestonesFrom(45));
        }

        @Test
        void anOrdinarySecondAnnouncesNothing() {
            assertEquals(OptionalInt.empty(), clockAt(1000).tick());
        }

        @Test
        void theSecondItselfIsWhatGetsReported() {
            assertEquals(OptionalInt.of(300), clockAt(301).tick());
            assertEquals(OptionalInt.of(1), clockAt(2).tick());
        }

        /** Expiry is not a milestone: nothing announces "0 seconds left". */
        @Test
        void reachingZeroAnnouncesNothing() {
            RoundClock clock = clockAt(1);

            assertEquals(OptionalInt.empty(), clock.tick());
            assertTrue(clock.expired());
        }

        @Test
        void everyMilestoneIsAnnouncedExactlyOnce() {
            List<Integer> announced = milestonesFrom(60 * 60);

            assertEquals(announced.size(), announced.stream().distinct().count());
        }
    }

    @Nested
    class FinalCountdown {

        @Test
        void coversTheLastFiveSecondsOnly() {
            assertTrue(RoundClock.isFinalCountdown(5));
            assertTrue(RoundClock.isFinalCountdown(1));
            assertFalse(RoundClock.isFinalCountdown(10));
            assertFalse(RoundClock.isFinalCountdown(300));
        }

        /** Zero is expiry, which has its own announcement, not a countdown number. */
        @Test
        void doesNotIncludeZero() {
            assertFalse(RoundClock.isFinalCountdown(0));
        }
    }
}
