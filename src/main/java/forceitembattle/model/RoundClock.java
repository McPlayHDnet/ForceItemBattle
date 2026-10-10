package forceitembattle.model;

import java.util.OptionalInt;
import java.util.Set;

/** Not thread-safe; the timer task is synchronous. */
public final class RoundClock {

    /** The seconds that get a title and a sound. */
    private static final Set<Integer> ANNOUNCED_SECONDS = Set.of(300, 60, 30, 10, 5, 4, 3, 2, 1);

    private int secondsLeft;

    private int totalSeconds;

    public int secondsLeft() {
        return this.secondsLeft;
    }

    public int totalSeconds() {
        return this.totalSeconds;
    }

    /** Pause-aware, because the clock itself is. */
    public int elapsedSeconds() {
        return this.totalSeconds - this.secondsLeft;
    }

    public void startRound(int totalSeconds) {
        this.totalSeconds = totalSeconds;
        this.secondsLeft = totalSeconds;
    }

    public void setSecondsLeft(int secondsLeft) {
        this.secondsLeft = secondsLeft;
    }

    /** Checked after {@link #tick()}, never instead of it. */
    public boolean expired() {
        return this.secondsLeft <= 0;
    }

    /** @return the second reached when worth announcing; expiry is not a milestone, see {@link #expired()} */
    public OptionalInt tick() {
        this.secondsLeft--;

        return ANNOUNCED_SECONDS.contains(this.secondsLeft)
                ? OptionalInt.of(this.secondsLeft)
                : OptionalInt.empty();
    }

    /** The final countdown is shown as a bare headline number rather than a line of warning text. */
    public static boolean isFinalCountdown(int secondsLeft) {
        return secondsLeft > 0 && secondsLeft <= 5;
    }
}
