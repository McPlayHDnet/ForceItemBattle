package forceitembattle.model;

public enum GameState {

    PRE_GAME,
    /**
     * The /start countdown. Teams and force items are already assigned here, so this is deliberately
     * <i>not</i> PRE_GAME: the roster is locked and anything gated on {@code isPreGame()} — team
     * editing, the spectate toggle, dropping a disconnecting player from the roster — must stay shut.
     */
    STARTING,
    PAUSED_GAME,
    MID_GAME,
    END_GAME;

    /**
     * Play is live: the clock is ticking and a find counts.
     *
     * <p>Excludes a pause, which is what almost every gameplay gate wants — you cannot find an
     * item, spend a joker or vote to skip while the game is halted.
     */
    public boolean roundRunning() {
        return this == MID_GAME;
    }

    /**
     * The round has started and has not finished, whether or not it is paused.
     *
     * <p>The distinction this pair exists to make. PAUSED_GAME is a sibling of MID_GAME rather than
     * a flag on it, so the old {@code isMidGame()} silently meant "and not while paused" — a
     * decision fifty-one call sites were making by omission, none of them recording whether they
     * meant it.
     *
     * <p><b>A pause stops this plugin's clock, not the world's.</b> Blocks still tick, primed TNT
     * still detonates, lava still flows and fire still spreads. Anything guarding the world rather
     * than gating play wants this, not {@link #roundRunning()}.
     *
     * <p>The rule lives here rather than on {@code Gamemanager} because which phases count as a
     * round is a fact about the phases. It also means it can be asked without a plugin.
     */
    public boolean roundInProgress() {
        return this == MID_GAME || this == PAUSED_GAME;
    }
}
