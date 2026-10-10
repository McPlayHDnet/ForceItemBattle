package forceitembattle.randomevents;

import forceitembattle.model.Find;

public interface RandomEvent {

    /** Fired once when the event starts. Announce it here. */
    void start();

    /** An instant event must not hold the single active slot, or it would swallow every remaining slot. */
    default boolean isInstant() {
        return false;
    }

    /** @return true when the event has concluded and should be cleared */
    default boolean onFoundItem(Find find) {
        return false;
    }

    /**
     * Mid-game only, so a countdown driven from here freezes during a pause.
     *
     * @return true when the event has concluded and should be cleared
     */
    default boolean tick() {
        return false;
    }

    /** The round ended (or the plugin shut down) with this event still running. */
    default void cancel() {
    }

    /** MiniMessage, identical for every player, starting with a blank line. */
    default String tabFooterBlock() {
        return "";
    }
}
