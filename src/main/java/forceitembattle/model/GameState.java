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
     * Play is live: the clock is ticking and a find counts. Excludes a pause, which is what almost
     * every gameplay gate wants — no finding, spending a joker or voting to skip while halted.
     */
    public boolean roundRunning() {
        return this == MID_GAME;
    }

    /**
     * The round has started and has not finished, paused or not.
     *
     * <p><b>A pause stops this plugin's clock, not the world's.</b> Blocks still tick, primed TNT
     * still detonates, lava still flows and fire still spreads. Anything guarding the <em>world</em>
     * rather than gating play wants this, not {@link #roundRunning()}.
     */
    public boolean roundInProgress() {
        return this == MID_GAME || this == PAUSED_GAME;
    }
}
