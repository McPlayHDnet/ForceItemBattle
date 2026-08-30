package forceitembattle.model;

/**
 * Where the round is, and nothing else.
 *
 * <h2>Why this is its own module</h2>
 *
 * <p>It was a field on {@code Gamemanager}, which meant that asking "is the round running?" — the
 * most-asked question in the codebase, 26 call sites across 20 files — created a dependency on the
 * class that starts and finishes rounds. Seven of the nine dependency cycles among managers existed
 * for that reason and no other: measuring the graph showed that of the twenty edges pointing into
 * {@code Gamemanager}, eleven wanted the roster and seven wanted this.
 *
 * <p>So, like {@link Roster}, this depends on nothing — no Bukkit, no plugin, no managers — and
 * that is load-bearing rather than incidental. A dependency added here would reintroduce a cycle
 * for every module that asks what phase the round is in, which is most of them.
 *
 * <p>The phase predicates live on {@link GameState} itself; this holds the current one and lets it
 * be moved. Nine of the ten listeners now hold one of these instead of a {@code Gamemanager},
 * because where the round is was the only thing they ever wanted from it.
 */
public final class RoundPhase {

    private GameState state = GameState.PRE_GAME;

    public GameState state() {
        return this.state;
    }

    /**
     * Moves the round to a new phase.
     *
     * <p>Deliberately not called {@code setState}: the transitions are not interchangeable writes.
     * {@code /start} freezes the roster by moving to STARTING, a pause has bookkeeping attached on
     * either side, and finishing a round is what the whole stats pipeline hangs off. Those belong
     * to {@code Gamemanager}, which orchestrates the effects and calls this for the flip.
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

    /**
     * The round has started and has not finished, pause included.
     *
     * <p>A pause stops this plugin's clock, not the world's — see {@link GameState#roundInProgress}.
     */
    public boolean roundInProgress() {
        return this.state.roundInProgress();
    }
}
