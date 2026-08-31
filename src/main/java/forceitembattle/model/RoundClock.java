package forceitembattle.model;

import java.util.OptionalInt;
import java.util.Set;

/**
 * How much of the round is left, and which seconds are worth announcing. See
 * {@code CONTEXT.md § Round Clock}.
 *
 * <p>Nothing here touches Bukkit; {@code TimerManager} drives it once a second and renders whatever
 * it reports. Not thread-safe and does not need to be — the timer task is a synchronous Bukkit
 * task, so every call arrives on the main thread.
 */
public final class RoundClock {

    /**
     * The seconds that get a title and a sound. Five minutes and one minute are warnings; thirty
     * and ten are the last chance to reposition; five down to one is the final countdown, which the
     * presenter renders differently because at that point the number <em>is</em> the message.
     */
    private static final Set<Integer> ANNOUNCED_SECONDS = Set.of(300, 60, 30, 10, 5, 4, 3, 2, 1);

    private int secondsLeft;

    /**
     * The round's full length. Held here because everything that wants it wants it <em>with</em>
     * the time remaining, to work out how far in we are — that is {@link #elapsedSeconds}.
     */
    private int totalSeconds;

    public int secondsLeft() {
        return this.secondsLeft;
    }

    public int totalSeconds() {
        return this.totalSeconds;
    }

    /** How far into the round we are. Pause-aware, because the clock itself is. */
    public int elapsedSeconds() {
        return this.totalSeconds - this.secondsLeft;
    }

    /** Starts a round of {@code totalSeconds}, with all of it remaining. */
    public void startRound(int totalSeconds) {
        this.totalSeconds = totalSeconds;
        this.secondsLeft = totalSeconds;
    }

    public void setSecondsLeft(int secondsLeft) {
        this.secondsLeft = secondsLeft;
    }

    /** True once the round is over. Checked after {@link #tick()}, never instead of it. */
    public boolean expired() {
        return this.secondsLeft <= 0;
    }

    /**
     * Advances the round by one second.
     *
     * @return the second reached, when it is one worth announcing; empty otherwise. Expiry is not
     *         a milestone — nothing announces "0 seconds left" — so the caller asks
     *         {@link #expired()} separately.
     */
    public OptionalInt tick() {
        this.secondsLeft--;

        return ANNOUNCED_SECONDS.contains(this.secondsLeft)
                ? OptionalInt.of(this.secondsLeft)
                : OptionalInt.empty();
    }

    /**
     * Whether a milestone is part of the final countdown, where the number is shown on its own as
     * the headline rather than as a line of warning text.
     */
    public static boolean isFinalCountdown(int secondsLeft) {
        return secondsLeft > 0 && secondsLeft <= 5;
    }
}
