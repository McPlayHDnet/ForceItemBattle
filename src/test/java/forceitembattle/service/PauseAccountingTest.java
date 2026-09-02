package forceitembattle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Subtracting paused time from an item's find window.
 *
 * Item durations are wall-clock deltas between hand-ins, so without this a pause between two
 * hand-ins counts as time spent searching — a 22-minute pause once turned a single item into a
 * 30-minute "timesink" that never happened, and put it top of the match stats.
 */
class PauseAccountingTest {

    private static List<long[]> pauses(long... bounds) {
        List<long[]> intervals = new java.util.ArrayList<>();
        for (int i = 0; i < bounds.length; i += 2) {
            intervals.add(new long[]{bounds[i], bounds[i + 1]});
        }
        return intervals;
    }

    @Test
    void noPausesMeansNothingIsSubtracted() {
        assertEquals(0, MatchHistoryReporter.pausedMillisWithin(List.of(), 0, 10_000));
    }

    @Test
    void aPauseFullyInsideTheWindowCountsWhole() {
        assertEquals(3_000, MatchHistoryReporter.pausedMillisWithin(pauses(4_000, 7_000), 0, 10_000));
    }

    @Test
    void aPauseEntirelyOutsideTheWindowCountsNothing() {
        assertEquals(0, MatchHistoryReporter.pausedMillisWithin(pauses(20_000, 30_000), 0, 10_000));
    }

    @Test
    void aPauseOverlappingTheStartCountsOnlyTheOverlap() {
        // pause 0..6000, window starts at 5000 -> 1000ms of overlap
        assertEquals(1_000, MatchHistoryReporter.pausedMillisWithin(pauses(0, 6_000), 5_000, 10_000));
    }

    @Test
    void aPauseOverlappingTheEndCountsOnlyTheOverlap() {
        // pause 9000..20000, window ends at 10000 -> 1000ms of overlap
        assertEquals(1_000, MatchHistoryReporter.pausedMillisWithin(pauses(9_000, 20_000), 0, 10_000));
    }

    @Test
    void aPauseSpanningTheWholeWindowSwallowsIt() {
        assertEquals(10_000, MatchHistoryReporter.pausedMillisWithin(pauses(0, 60_000), 0, 10_000));
    }

    @Test
    void severalPausesInOneWindowAllCount() {
        assertEquals(3_500,
                MatchHistoryReporter.pausedMillisWithin(pauses(1_000, 2_000, 4_000, 6_000, 8_000, 8_500), 0, 10_000));
    }

    @Test
    void anEmptyWindowSubtractsNothing() {
        assertEquals(0, MatchHistoryReporter.pausedMillisWithin(pauses(0, 60_000), 5_000, 5_000));
    }

    /**
     * The recorded pauses are wall-clock and a match can outlive a clock adjustment, so a window
     * that runs backwards must not produce a negative subtraction.
     */
    @Test
    void aBackwardsWindowSubtractsNothing() {
        assertEquals(0, MatchHistoryReporter.pausedMillisWithin(pauses(0, 60_000), 9_000, 1_000));
    }

    @Test
    void openAndCloseRecordAPauseThatIsThenSubtracted() throws Exception {
        MatchHistoryReporter reporter = new MatchHistoryReporter(null, null, null, null);
        reporter.beginMatch(java.util.UUID.randomUUID());

        long before = System.currentTimeMillis();
        reporter.onPaused();
        Thread.sleep(30);
        reporter.onResumed();
        long after = System.currentTimeMillis();

        // the closed interval lands inside [before, after] and is non-zero
        long paused = invokeInstanceOverlap(reporter, before, after);
        org.junit.jupiter.api.Assertions.assertTrue(paused >= 20,
                "expected the recorded pause to be subtracted, got " + paused + "ms");
    }

    /** beginMatch clears anything the previous round recorded. */
    @Test
    void beginMatchDiscardsEarlierPauses() throws Exception {
        MatchHistoryReporter reporter = new MatchHistoryReporter(null, null, null, null);
        reporter.beginMatch(java.util.UUID.randomUUID());

        long before = System.currentTimeMillis();
        reporter.onPaused();
        Thread.sleep(20);
        reporter.onResumed();

        reporter.beginMatch(java.util.UUID.randomUUID());

        assertEquals(0, invokeInstanceOverlap(reporter, before, System.currentTimeMillis()));
    }

    private static long invokeInstanceOverlap(MatchHistoryReporter reporter, long from, long to)
            throws Exception {
        var method = MatchHistoryReporter.class
                .getDeclaredMethod("pausedMillisWithin", long.class, long.class);
        method.setAccessible(true);
        return (long) method.invoke(reporter, from, to);
    }
}
