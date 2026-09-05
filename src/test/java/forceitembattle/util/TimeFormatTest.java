package forceitembattle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeFormatTest {

    // ==================== durations ====================

    @Test
    void durationsDropTheComponentsThatAreZero() {
        assertEquals("30s", TimeFormat.humanised(30));
        assertEquals("5m 30s", TimeFormat.humanised(330));
        assertEquals("1h 1m 1s", TimeFormat.humanised(3661));
        assertEquals("2h 30s", TimeFormat.humanised(7230));
    }

    /**
     * Hours and minutes are written with a trailing space, so a duration that ends on one keeps it.
     * Pinned rather than fixed: it has always done this, and the strings land inside MiniMessage
     * where the space is invisible — but a future tidy-up should be a decision, not a surprise.
     */
    @Test
    void aDurationEndingOnHoursOrMinutesKeepsATrailingSpace() {
        assertEquals("5m ", TimeFormat.humanised(300));
        assertEquals("1h ", TimeFormat.humanised(3600));
    }

    /**
     * Long-standing behaviour rather than a decision, pinned so a change to it is a deliberate one.
     * The callers get away with it because a round showing zero is already over.
     */
    @Test
    void aZeroDurationRendersAsNothingAtAll() {
        assertEquals("", TimeFormat.humanised(0));
    }

    // ==================== countdown phrasing ====================

    @Test
    void wholeMinutesAreSpokenInMinutes() {
        assertEquals("5 minutes left", TimeFormat.countdownPhrase(300));
        assertEquals("1 minute left", TimeFormat.countdownPhrase(60));
    }

    @Test
    void everythingElseIsSpokenInSeconds() {
        assertEquals("30 seconds left", TimeFormat.countdownPhrase(30));
        assertEquals("10 seconds left", TimeFormat.countdownPhrase(10));
    }

    // ==================== countdowns ====================

    @Test
    void countdownsRenderAsMinutesAndSeconds() {
        assertTrue(TimeFormat.colored(0).endsWith("00:00"));
        assertTrue(TimeFormat.colored(59).endsWith("00:59"));
        assertTrue(TimeFormat.colored(600).endsWith("10:00"));
        assertTrue(TimeFormat.colored(3599).endsWith("59:59"));
    }

    @Test
    void countdownColourEscalatesAsTimeRunsOut() {
        assertTrue(TimeFormat.colored(600).startsWith("<green>"));
        assertTrue(TimeFormat.colored(120).startsWith("<gold>"));
        assertTrue(TimeFormat.colored(30).startsWith("<red>"));
        assertTrue(TimeFormat.colored(10).startsWith("<dark_red>"));
    }

    @Test
    void negativeCountdownsClampToZero() {
        assertEquals(TimeFormat.colored(0), TimeFormat.colored(-500));
    }

    // ==================== world clock ====================

    /**
     * Minecraft's day starts at sunrise: tick 0 is 06:00, not midnight. Reading the tick count
     * straight off as a clock is six hours out, which is the difference between "it's morning" and
     * "it's about to be dark".
     */
    @Test
    void tickZeroIsSunriseNotMidnight() {
        assertEquals("06:00", TimeFormat.worldClock(0));
    }

    @Test
    void theKnownPointsOfTheDayLineUp() {
        assertEquals("12:00", TimeFormat.worldClock(6000));   // noon
        assertEquals("18:00", TimeFormat.worldClock(12000));  // sunset
        assertEquals("00:00", TimeFormat.worldClock(18000));  // midnight
        assertEquals("05:00", TimeFormat.worldClock(23000));  // just before sunrise
    }

    @Test
    void minutesAdvanceWithinAnHour() {
        assertEquals("06:30", TimeFormat.worldClock(500));
        assertEquals("07:00", TimeFormat.worldClock(1000));
    }

    @Test
    void aFullDayWrapsBackToSunrise() {
        assertEquals("06:00", TimeFormat.worldClock(24000));
        assertEquals(TimeFormat.worldClock(6000), TimeFormat.worldClock(24000 + 6000));
    }

    /** getTime() is always in range, but a full-time value or a clock change must not blow up. */
    @Test
    void outOfRangeTicksStillProduceAValidClock() {
        assertEquals("06:00", TimeFormat.worldClock(-24000));
        assertTrue(TimeFormat.worldClock(Long.MAX_VALUE / 60).matches("\\d{2}:\\d{2}"));
    }
}
