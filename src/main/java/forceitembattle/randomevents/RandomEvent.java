package forceitembattle.randomevents;

import forceitembattle.model.Find;

public interface RandomEvent {

    /**
     * Fired once when the event starts. Announce it here.
     */
    void start();

    /**
     * Whether the event is over the moment {@link #start()} returns. An instant event leaves
     * nothing running that anyone can win, so it must not hold the single active-event slot —
     * it would swallow every remaining slot in the round.
     */
    default boolean isInstant() {
        return false;
    }

    /**
     * Routed from FoundItemResolver for every find while this event is running.
     *
     * @return true when the event has concluded and should be cleared.
     */
    default boolean onFoundItem(Find find) {
        return false;
    }

    /**
     * Routed once per second from RandomEventManager while this event holds the active slot.
     * Because that tick runs mid-game only, a countdown driven from here freezes during pause
     * with no extra handling.
     *
     * @return true when the event has concluded and should be cleared.
     */
    default boolean tick() {
        return false;
    }

    /**
     * The round ended (or the plugin shut down) with this event still running.
     */
    default void cancel() {
    }

    /**
     * This event's contribution to the tab footer, as MiniMessage, refreshed once a second while
     * the event holds the active slot. Empty by default — an event with nothing ongoing to report
     * (an instant one, or one that resolves on the next find) shows nothing.
     *
     * The same for every player, so it is built once per refresh rather than per viewer. Blocks
     * start with a blank line to separate them from what precedes; see
     * {@link forceitembattle.manager.TabListManager} for the surrounding layout.
     */
    default String tabFooterBlock() {
        return "";
    }
}
