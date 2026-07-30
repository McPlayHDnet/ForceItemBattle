package forceitembattle.randomevents;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;

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
     * Routed from FoundItemListener for every find while this event is running.
     *
     * @return true when the event has concluded and should be cleared.
     */
    default boolean onFoundItem(FoundItemEvent foundItemEvent, ForceItemPlayer forceItemPlayer) {
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
}
