package forceitembattle.randomevents;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;

public interface RandomEvent {

    /**
     * Fired once when the event starts. Announce it here.
     */
    void start();

    /**
     * Routed from FoundItemListener for every find while this event is running.
     *
     * @return true when the event has concluded and should be cleared.
     */
    boolean onFoundItem(FoundItemEvent foundItemEvent, ForceItemPlayer forceItemPlayer);

    /**
     * The round ended (or the plugin shut down) with this event still running.
     */
    default void cancel() {
    }
}
