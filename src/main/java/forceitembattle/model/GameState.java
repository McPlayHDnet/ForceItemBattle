package forceitembattle.model;

public enum GameState {

    PRE_GAME,
    /** Teams and items are already assigned, so this is not PRE_GAME: the roster is locked. */
    STARTING,
    PAUSED_GAME,
    MID_GAME,
    END_GAME;

    /** Excludes a pause, which is what almost every gameplay gate wants. */
    public boolean roundRunning() {
        return this == MID_GAME;
    }

    /** Includes a pause: guards on the world want this, since the world is frozen, not unprotected. */
    public boolean roundInProgress() {
        return this == MID_GAME || this == PAUSED_GAME;
    }
}
