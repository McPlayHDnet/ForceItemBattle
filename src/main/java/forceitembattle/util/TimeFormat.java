package forceitembattle.util;

/**
 * Countdown rendering for the surfaces that show one — the tab footer's pool and trader timers, and
 * a running random event's own clock.
 *
 * Kept in one place so every countdown in the game turns red at the same moment; a player reading
 * two timers side by side in the same footer should not have to learn two colour scales.
 */
public final class TimeFormat {

    private TimeFormat() {
    }

    /**
     * {@code mm:ss}, coloured by how much is left: green above two minutes, amber under two,
     * red under thirty seconds, dark red in the last ten. Negative input clamps to zero.
     */
    public static String colored(int remainingSeconds) {
        int seconds = Math.max(remainingSeconds, 0);
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);

        String color;
        if (seconds <= 10) {
            color = "<dark_red>";
        } else if (seconds <= 30) {
            color = "<red>";
        } else if (seconds <= 120) {
            color = "<gold>";
        } else {
            color = "<green>";
        }

        return color + time;
    }

    /**
     * A world's time of day as {@code HH:mm}.
     *
     * Minecraft's day starts at sunrise, not midnight: tick 0 is 06:00 and a full day is 24000
     * ticks, so the six-hour offset has to be added before wrapping. Reading the raw tick count as
     * a clock is off by a quarter of a day, which is exactly enough to make "it's about to get
     * dark" wrong.
     */
    public static String worldClock(long timeTicks) {
        long minutesOfDay = Math.floorMod((timeTicks * 60 / 1000) + 6 * 60, 1440L);
        return String.format("%02d:%02d", minutesOfDay / 60, minutesOfDay % 60);
    }
}
