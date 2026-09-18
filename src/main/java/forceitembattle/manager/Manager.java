package forceitembattle.manager;

/**
 * A stateful subsystem with a lifecycle, built in {@code ForceItemBattle.onEnable()}.
 *
 * <p>Construction order and lifecycle order are two different lists — see
 * {@code ForceItemBattle.lifecycleOrder()}.
 */
public interface Manager {

    /**
     * Called after all managers have been constructed. Safe to use any sibling here — this is the
     * first point at which that is true.
     */
    default void enable() {
    }

    /** Called on plugin shutdown, in reverse lifecycle order. Cancel tasks and flush state here. */
    default void disable() {
    }
}
