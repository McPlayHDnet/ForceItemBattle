package forceitembattle.manager;

public interface Manager {

    /**
     * Called after all managers have been constructed. Safe to use any sibling here.
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