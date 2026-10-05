package forceitembattle.model;

public enum Admission {

    /** {@code startSetupApplied} decides whether the round setup still has to be applied. */
    RETURNING_PARTICIPANT,

    /** On the roster, and the round is over. They missed the result screen and are handed it. */
    RESULT_SCREEN,

    /** Reconnecting during PRE_GAME or the countdown; the player object is reattached and that is all. */
    RECONNECTING_BEFORE_START,

    /** The roster froze at the countdown, so this is the one outcome that creates no roster entry. */
    LATE_SPECTATOR,

    /** Gets a roster entry flagged as a spectator, so they hold a place. */
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
