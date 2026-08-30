package forceitembattle.manager;

/**
 * A stateful subsystem with a lifecycle, built in {@code ForceItemBattle.onEnable()}.
 *
 * <h2>Why most of these take the plugin</h2>
 *
 * Managers are constructed in registration order, so at the moment one is built, every manager
 * registered after it is still null. A manager therefore <em>cannot</em> generally take its
 * siblings as constructor parameters — which is why they take {@code ForceItemBattle} and reach
 * through it later, once everything exists.
 *
 * <p>That is a real constraint, not a habit, and it does not apply to a manager registered after
 * everything it needs. {@code FoundItemResolver} is registered last and names its eight
 * collaborators outright; anything new that can be built late should do the same. Moving an
 * existing manager later to gain the same is possible but is not free — registration order is also
 * {@code enable()} order, and its reverse is {@code disable()} order.
 *
 * <h2>Why the sweep stopped where it did</h2>
 *
 * <p>Listeners and commands are built after every manager exists, so they have no such constraint;
 * the listeners now name their collaborators and reach through the plugin zero times. Six managers
 * followed, and three of them ({@code ProtectionManager}, {@code VoteSkipManager},
 * {@code TabListManager}) no longer know a plugin exists at all.
 *
 * <p>The rest cannot follow, and it is worth being exact about why, because two architecture passes
 * have now guessed at it. There are <b>nine mutual dependencies</b> among managers — each pair
 * calls into the other, so neither can hold the other as a final field:
 *
 * <pre>
 *   Gamemanager &lt;-&gt; ItemDifficultiesManager, ScoreboardManager, AchievementManager,
 *                  WanderingTraderManager, BackpackManager, TeamsManager, RandomEventManager
 *   TimerManager &lt;-&gt; ItemDifficultiesManager
 *   WanderingTraderManager &lt;-&gt; ScoreboardManager
 * </pre>
 *
 * <p>A cyclic graph has no topological order, so "construct in dependency order" — the obvious fix —
 * cannot work while those exist. Seven of the nine run through {@code Gamemanager}, which makes
 * this a decomposition problem rather than a wiring one: the sweep is blocked behind splitting the
 * round's core, not behind anything mechanical.
 *
 * <p>A second, smaller blocker sits underneath it: this bootstrap uses one ordering for both
 * construction and lifecycle. {@code CollectionManager} builds its two loaders in its constructor
 * and they want {@code FIBServiceClient}, which is registered later — reachable only by moving
 * registration, which would move {@code enable()} and {@code disable()} with it. Separating the two
 * orderings would unblock that class of case without touching a single manager.
 */
public interface Manager {

    /**
     * Called after all managers have been constructed. Safe to use any sibling here — this is the
     * first point at which that is true.
     */
    default void enable() {
    }

    /**
     * Called on plugin shutdown, in reverse registration order. Cancel tasks and
     * flush state here.
     */
    default void disable() {
    }
}
