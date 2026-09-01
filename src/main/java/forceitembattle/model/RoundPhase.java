package forceitembattle.model;

/**
 * Where the round is, and nothing else.
 *
 * <p>Its own module because "is the round running?" is the most-asked question in the codebase, and
 * answering it from {@code Gamemanager} made almost every listener depend on the class that starts
 * and finishes rounds. Like {@link Roster}, this depends on nothing — no Bukkit, no plugin, no
 * managers — and that is load-bearing: a dependency added here reintroduces a cycle for every module
 * that asks what phase the round is in, which is most of them.
 *
 * <p>The phase predicates live on {@link GameState} itself; this holds the current one.
 */
public final class RoundPhase {

    private GameState state = GameState.PRE_GAME;

    public GameState state() {
        return this.state;
    }

    /**
     * Deliberately not {@code setState}: the transitions are not interchangeable writes. Moving to
     * STARTING freezes the roster, a pause has bookkeeping on either side, and finishing is what the
     * stats pipeline hangs off. Those effects belong to {@code Gamemanager}; this is only the flip.
     */
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

    /** Play is live: the clock is ticking and a find counts. Excludes a pause. */
    public boolean roundRunning() {
        return this.state.roundRunning();
    }

    /** The round has started and has not finished, pause included. */
    public boolean roundInProgress() {
        return this.state.roundInProgress();
    }
}
