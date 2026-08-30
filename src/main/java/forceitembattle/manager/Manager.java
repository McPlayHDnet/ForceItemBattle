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
