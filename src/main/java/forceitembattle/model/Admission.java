package forceitembattle.model;

/**
 * What an arriving player becomes: six outcomes from two facts — already on the roster, and which
 * state the round is in. Answers in names; one adapter turns the name into game modes and slots.
 */
public enum Admission {

    /**
     * On the roster, and the round is running. They already own a team, a force item and a score.
     * {@code startSetupApplied} decides whether the round setup still has to be applied.
     */
    RETURNING_PARTICIPANT,

    /** On the roster, and the round is over. They missed the result screen and are handed it. */
    RESULT_SCREEN,

    /**
     * On the roster, before the round runs — reconnecting during PRE_GAME or the countdown. The
     * player object is reattached and that is the whole outcome.
     */
    RECONNECTING_BEFORE_START,

    /**
     * Not on the roster, and the round is running. A spectator for this round — the roster froze
     * when the countdown began. The one outcome that creates no roster entry.
     */
    LATE_SPECTATOR,

    /**
     * Not on the roster, and the countdown is running. Also a spectator for this round, but they
     * get an entry so they hold a place, flagged as a spectator.
     */
    COUNTDOWN_SPECTATOR,

    /** Not on the roster, and no round is running. A lobby player, and a participant in the next round. */
    LOBBY;

    public boolean joinsRoster() {
        return this == COUNTDOWN_SPECTATOR || this == LOBBY;
    }

    /** Whether the player watches the round now starting rather than holding a stake in it. */
    public boolean isSpectating() {
        return this == LATE_SPECTATOR || this == COUNTDOWN_SPECTATOR;
    }
}
