package forceitembattle.manager;

/** Construction order and lifecycle order differ; see {@code ForceItemBattle.lifecycleOrder()}. */
public interface Manager {

    /** Called once every manager is constructed, the first point at which any sibling is safe to use. */
    default void enable() {
    }

    /** Called on plugin shutdown, in reverse lifecycle order. Cancel tasks and flush state here. */
    default void disable() {
    }
}
