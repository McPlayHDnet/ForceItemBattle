package forceitembattle.model;

/**
 * What an arriving player becomes.
 *
 * <h2>Why this is a type</h2>
 *
 * <p>This was a five-branch tree inside {@code PlayerLifecycleListener.onPlayerJoin}, and every one
 * of its leaves built an {@code ItemStack} or set a game mode — so the rule and the inventory writes
 * were the same code, and the rule sat behind the headless wall. {@code Roster} is a term the
 * glossary already used with no module behind it; deciding who counts as a participant is what it
 * means.
 *
 * <p>The split is the one {@link RoundSetup} and {@code PlayerOutfitter} already proved: the rule
 * answers in names, one adapter turns the name into game modes and slots.
 *
 * <h2>The two that create a roster entry, and the one that does not</h2>
 *
 * <p>{@link #COUNTDOWN_SPECTATOR} and {@link #LOBBY} put the player on the roster;
 * {@link #LATE_SPECTATOR} does not, and nothing else ever adds one — {@code addPlayer} has exactly
 * two callers, both of them the join handler. That asymmetry is why {@code getForceItemPlayer}
 * returns null often enough for its callers to be full of null checks, and it means a player who
 * joined mid-round is not on the roster when the <em>next</em> round starts either. Production is
 * spared because {@code scheduleReset} restarts the JVM between rounds and everyone rejoins into
 * PRE_GAME; a server that plays two rounds in one session is not.
 *
 * <p><b>This is intended, not an oversight.</b> Someone who arrives mid-round is a spectator for
 * that round and holds no place in it, so having no roster entry is the accurate representation.
 * Giving them one would also put them inside {@code players().size()}, which is what {@code /start}
 * counts to decide whether four players are present for teams — it would change when teams get
 * built. Do not "even this up" with {@link #COUNTDOWN_SPECTATOR}: that one holds an entry because
 * the roster froze around it while it was already there, which is a different situation.
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
     * On the roster, before the round runs — reconnecting during PRE_GAME or the countdown.
     *
     * <p>Nothing to write. The player object is reattached and that is the whole outcome, which is
     * easy to lose when this is a branch that falls off the end of an if-chain rather than a name.
     */
    RECONNECTING_BEFORE_START,

    /**
     * Not on the roster, and the round is running. A spectator for this round — the roster froze
     * when the countdown began.
     *
     * <p>The one outcome that creates no roster entry. See the class note.
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
