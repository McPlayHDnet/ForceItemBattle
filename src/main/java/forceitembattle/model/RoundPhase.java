package forceitembattle.model;

/** Depends on nothing, deliberately: almost every module asks for the phase, so a dependency here creates cycles. */
public final class RoundPhase {

    private GameState state = GameState.PRE_GAME;

    public GameState state() {
        return this.state;
    }

    /** Not a setter: STARTING freezes the roster and finishing drives stats, effects that belong to Gamemanager. */
    public void moveTo(GameState state) {
        this.state = state;
    }

    public boolean isPreGame() {
        return this.state == GameState.PRE_GAME;
    }

    public boolean isStarting() {
        return this.state == GameState.STARTING;
    }

    public boolean isPausedGame() {
        return this.state == GameState.PAUSED_GAME;
    }

    public boolean isEndGame() {
        return this.state == GameState.END_GAME;
    }

    public boolean roundRunning() {
        return this.state.roundRunning();
    }

    public boolean roundInProgress() {
        return this.state.roundInProgress();
    }
}
