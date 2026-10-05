package forceitembattle.util;

/** One place, so every countdown in the game turns red at the same moment. */
public final class TimeFormat {

    private TimeFormat() {
    }

    /** Green above two minutes, amber under two, red under thirty seconds, dark red in the last ten. */
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

    /** Zero renders as "" and an hour/minute ending keeps a trailing space; both pinned by tests. */
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

    public static String countdownPhrase(int secondsLeft) {
        if (secondsLeft % 60 == 0) {
            int minutes = secondsLeft / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes") + " left";
        }
        return secondsLeft + " seconds left";
    }

    /** Tick 0 is 06:00, so the six-hour offset is added before wrapping. */
    public static String worldClock(long timeTicks) {
        long minutesOfDay = Math.floorMod((timeTicks * 60 / 1000) + 6 * 60, 1440L);
        return String.format("%02d:%02d", minutesOfDay / 60, minutesOfDay % 60);
    }
}
