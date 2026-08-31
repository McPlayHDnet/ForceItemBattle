package forceitembattle.model;

/**
 * What an arriving player becomes: six outcomes from two facts — already on the roster, and which
 * state the round is in. The table, and why {@link #LATE_SPECTATOR} deliberately creates no roster
 * entry, are in {@code CONTEXT.md § Roster}.
 *
 * <p>The rule answers in names; one adapter turns the name into game modes and slots, the way
 * {@link RoundSetup} and {@code PlayerOutfitter} already do.
 */
public enum Admission {

    /**
     * On the roster, and the round is running. They own a team, a force item and a score already,
     * so they come back as the participant they were. Someone who was offline for the whole
     * countdown still needs the round setup applied; someone who was online does not, and
     * {@code startSetupApplied} is what tells them apart.
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

    /** Whether this outcome puts the player on the roster. */
    public boolean joinsRoster() {
        return this == COUNTDOWN_SPECTATOR || this == LOBBY;
    }

    /** Whether the player holds a stake in the round now starting, rather than watching it. */
    public boolean isSpectating() {
        return this == LATE_SPECTATOR || this == COUNTDOWN_SPECTATOR;
    }
}
