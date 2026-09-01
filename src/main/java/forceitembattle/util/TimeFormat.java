package forceitembattle.util;

/**
 * Countdown rendering. Kept in one place so every countdown in the game turns red at the same
 * moment: a player reading two timers side by side should not have to learn two colour scales.
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
     * A duration as {@code 1h 5m 30s}, dropping any component that is zero.
     *
     * <p>Two warts, pinned by tests rather than fixed so changing either is a decision: zero renders
     * as the empty string, and a duration ending on hours or minutes keeps a trailing space
     * ({@code "5m "}). Callers get away with both — a round showing {@code 0} is already over, and
     * the strings land inside MiniMessage where the space is invisible.
     */
    public static String humanised(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 60 / 60;

        String time = "";
        if (hours != 0) time += hours + "h ";
        if (minutes != 0) time += minutes + "m ";
        if (seconds != 0) time += seconds + "s";

        return time;
    }

    /**
     * How a countdown milestone is spoken: {@code 5 minutes left}, {@code 1 minute left},
     * {@code 30 seconds left}. Whole minutes are said in minutes, everything else in seconds.
     */
    public static String countdownPhrase(int secondsLeft) {
        if (secondsLeft % 60 == 0) {
            int minutes = secondsLeft / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes") + " left";
        }
        return secondsLeft + " seconds left";
    }

    /**
     * A world's time of day as {@code HH:mm}. Minecraft's day starts at sunrise, not midnight — tick
     * 0 is 06:00 — so the six-hour offset has to be added before wrapping. Reading the raw tick
     * count as a clock is off by a quarter of a day.
     */
    public static String worldClock(long timeTicks) {
        long minutesOfDay = Math.floorMod((timeTicks * 60 / 1000) + 6 * 60, 1440L);
        return String.format("%02d:%02d", minutesOfDay / 60, minutesOfDay % 60);
    }
}
