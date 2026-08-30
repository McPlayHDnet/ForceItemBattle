package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The operator-notification rate limit.
 *
 * <p>Both defects this replaced are pinned as the behaviour they should have had: the suppression
 * expires, and the bookkeeping does not grow without bound. Neither was reachable before — the rule
 * was a field on a listener, checked inside an event handler.
 */
class AdminNotifierTest {

    /** A clock the test moves by hand. */
    private static final class FakeClock {
        private long now = 1_000_000L;

        long read() {
            return this.now;
        }

        void advance(long millis) {
            this.now += millis;
        }
    }

    private FakeClock clock;
    private AdminNotifier notifier;

    private void freshNotifier() {
        this.clock = new FakeClock();
        this.notifier = new AdminNotifier(this.clock::read);
    }

    @Nested
    class Suppressing {

        @Test
        void theFirstTimeAMessageAppearsItIsSent() {
            freshNotifier();

            assertTrue(notifier.claim("someone broke a bed"));
        }

        @Test
        void anImmediateRepeatIsSuppressed() {
            freshNotifier();
            notifier.claim("someone broke a bed");

            assertFalse(notifier.claim("someone broke a bed"));
        }

        @Test
        void aDifferentMessageIsNotSuppressed() {
            freshNotifier();
            notifier.claim("someone broke a bed");

            assertTrue(notifier.claim("someone opened a chest"));
        }

        @Test
        void repeatsStaySuppressedForTheWholeWindow() {
            freshNotifier();
            notifier.claim("someone broke a bed");

            clock.advance(AdminNotifier.REPEAT_WINDOW_MILLIS - 1);

            assertFalse(notifier.claim("someone broke a bed"));
        }

        /**
         * The defect this class exists to fix: the old rule kept every message forever, so the
         * second offence was never reported — not later in the round, not in a later round.
         */
        @Test
        void theSameOffenceIsReportedAgainOnceTheWindowPasses() {
            freshNotifier();
            notifier.claim("someone broke a bed");

            clock.advance(AdminNotifier.REPEAT_WINDOW_MILLIS);

            assertTrue(notifier.claim("someone broke a bed"));
        }
    }

    @Nested
    class Bookkeeping {

        @Test
        void aTrackedMessageIsRememberedWhileItIsSuppressed() {
            freshNotifier();
            notifier.claim("a");
            notifier.claim("b");

            assertEquals(2, notifier.trackedMessages());
        }

        /**
         * The other defect: messages carry names and coordinates, so the set of distinct strings is
         * effectively unbounded and the old list grew for the life of the server.
         */
        @Test
        void expiredMessagesAreForgottenRatherThanAccumulating() {
            freshNotifier();
            for (int i = 0; i < 500; i++) {
                notifier.claim("attempt at block " + i);
            }
            assertEquals(500, notifier.trackedMessages());

            clock.advance(AdminNotifier.REPEAT_WINDOW_MILLIS);
            notifier.claim("something new");

            assertEquals(1, notifier.trackedMessages());
        }
    }
}
