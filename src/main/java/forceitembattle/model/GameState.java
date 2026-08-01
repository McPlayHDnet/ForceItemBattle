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
    END_GAME

}
