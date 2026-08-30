package forceitembattle.manager;

import forceitembattle.manager.ItemDifficultiesManager.State;
import forceitembattle.settings.QuickieMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * When each item pool opens during a round.
 *
 * <h2>Why this is its own object</h2>
 *
 * The schedule is the subtlest thing {@link ItemDifficultiesManager} does — two different policies
 * chosen by round length, percentages that have to be turned into minute marks, and a quickie mode
 * that can cap the whole thing — and it was reachable only through a manager holding 1,382 lines of
 * item data, which in turn needed a mocked plugin, timer and game manager to answer "is MID open
 * yet?". None of that is a property of the question.
 *
 * <p>Everything here takes the clock as arguments rather than reading it. The manager still owns
 * the clock, the settings and the items; this owns only the arithmetic.
 */
public final class UnlockSchedule {

    /**
     * Rounds at or above this length use fixed minute marks for the MID/LATE unlocks instead of
     * percentages, so a long game doesn't hold the later pools back for half an hour.
     */
    static final int FIXED_SCHEDULE_MIN_MINUTES = 50;
    static final int FIXED_MID_UNLOCK_MINUTES = 5;
    static final int FIXED_LATE_UNLOCK_MINUTES = 15;

    private final Map<State, Double> percentages = new EnumMap<>(State.class);

    private UnlockSchedule(double early, double mid, double late) {
        this.percentages.put(State.EARLY, early);
        this.percentages.put(State.MID, mid);
        this.percentages.put(State.LATE, late);
    }

    /**
     * Percentage-based unlock points, used for any round short enough that fixed minute marks would
     * put a pool past the end of the game.
     */
    public static UnlockSchedule percentageBased() {
        return new UnlockSchedule(0, 11.11, 28.88);
    }

    /**
     * The schedule for a round of {@code durationMinutes}.
     *
     * <p>Long rounds get fixed marks — MID at 5 minutes, LATE at 15 — because on a 90-minute game
     * the percentage schedule would hold LATE back for 26 minutes. Below that threshold the
     * percentages keep the three pools spread across the round instead of bunched at the start.
     */
    public static UnlockSchedule forRound(int durationMinutes) {
        if (durationMinutes < FIXED_SCHEDULE_MIN_MINUTES) {
            return percentageBased();
        }
        return new UnlockSchedule(0,
                (FIXED_MID_UNLOCK_MINUTES / (double) durationMinutes) * 100,
                (FIXED_LATE_UNLOCK_MINUTES / (double) durationMinutes) * 100);
    }

    /**
     * The minute mark a pool opens at in a round of {@code durationSeconds}. Every caller that needs
     * an unlock time goes through here, so the schedule is only defined once.
     */
    public int unlockMinute(State state, int durationSeconds) {
        double percentage = this.percentages.getOrDefault(state, 0d);
        return (int) Math.round((durationSeconds * (percentage / 100)) / 60);
    }

    /**
     * Pools active at this point in the round: permitted by the quickie mode and unlocked by
     * elapsed time. Returned in unlock order (EARLY → LATE).
     */
    public List<State> activeAt(int elapsedMinutes, int durationSeconds, QuickieMode quickieMode) {
        List<State> active = new ArrayList<>();
        for (State state : State.VALUES) {
            if (allows(quickieMode, state) && elapsedMinutes >= this.unlockMinute(state, durationSeconds)) {
                active.add(state);
            }
        }
        return active;
    }

    /**
     * The next pool scheduled to open, or {@code null} when none remain — every permitted pool is
     * already active, which is also what a capping quickie mode looks like from here.
     */
    @Nullable
    public State nextAfter(int elapsedMinutes, int durationSeconds, QuickieMode quickieMode) {
        for (State state : State.VALUES) {
            if (allows(quickieMode, state) && this.unlockMinute(state, durationSeconds) > elapsedMinutes) {
                return state;
            }
        }
        return null;
    }

    /**
     * Seconds until the next pool opens, or {@code -1} when none remain. Reaches 0 on the same tick
     * that pool becomes active.
     */
    public int secondsUntilNext(int elapsedSeconds, int durationSeconds, QuickieMode quickieMode) {
        State next = this.nextAfter(elapsedSeconds / 60, durationSeconds, quickieMode);
        if (next == null) {
            return -1;
        }
        return Math.max(0, this.unlockMinute(next, durationSeconds) * 60 - elapsedSeconds);
    }

    /** Whether the quickie mode lets this pool open at all. */
    static boolean allows(QuickieMode quickieMode, State state) {
        return switch (quickieMode) {
            case DISABLED -> true;
            case EARLY -> state == State.EARLY;
            case EARLY_MID -> state == State.EARLY || state == State.MID;
        };
    }
}
