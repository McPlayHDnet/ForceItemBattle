package forceitembattle.settings.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class SkipHandler implements AchievementHandler<SkipProgress> {

    private final int targetSkips;
    private final boolean requireConsecutive;
    private final long withinSeconds;

    public SkipHandler(int targetSkips, boolean requireConsecutive, long withinSeconds) {
        this.targetSkips = targetSkips;
        this.requireConsecutive = requireConsecutive;
        this.withinSeconds = withinSeconds;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.SKIP_ITEM;
    }

    @Override
    public boolean check(Event event, SkipProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        // CRITICAL FIX: On first event, initialize the timestamp
        // This handles the case where the first item is assigned at game start
        if (progress.firstEvent) {
            progress.itemReceivedTime = System.currentTimeMillis();
            progress.firstEvent = false;

            // If this first event is a skip, we can't know how long the item was assigned
            // So we don't trigger any time-based achievement on the very first skip
            if (foundEvent.isSkipped() && withinSeconds > 0) {
                // Update timestamp for next item and return false
                progress.itemReceivedTime = System.currentTimeMillis();
                return false;
            }
        }

        boolean result = false;

        if (foundEvent.isSkipped()) {
            // Check time window for the item being skipped
            if (withinSeconds > 0) {
                long timeSinceReceived = (System.currentTimeMillis() - progress.itemReceivedTime) / 1000;
                if (timeSinceReceived <= withinSeconds) {
                    progress.skipCount++;
                    result = progress.skipCount >= targetSkips;
                } else {
                    // Outside time window
                    if (requireConsecutive) {
                        progress.skipCount = 0;
                    }
                }
            } else {
                // No time constraint
                progress.skipCount++;
                result = progress.skipCount >= targetSkips;
            }
        } else {
            // Found (not skipped)
            if (requireConsecutive) {
                progress.skipCount = 0;
            }
        }

        // Always update timestamp after finding or skipping
        // because a new item is assigned immediately
        progress.itemReceivedTime = System.currentTimeMillis();

        return result;
    }

    @Override
    public SkipProgress createProgress() {
        return new SkipProgress();
    }
}