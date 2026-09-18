package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SkipAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

public class SkipAchievementHandler implements AchievementHandler<SkipAchievementProgress> {

    private final int targetSkips;
    private final boolean requireConsecutive;
    private final long withinSeconds;

    public SkipAchievementHandler(int targetSkips, boolean requireConsecutive, long withinSeconds) {
        this.targetSkips = targetSkips;
        this.requireConsecutive = requireConsecutive;
        this.withinSeconds = withinSeconds;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.SKIP_ITEM;
    }

    @Override
    public boolean check(Event event, SkipAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        int secondsLeft = world.secondsLeft();

        // On the first event, anchor the received marker to the round start
        // (the first item is assigned at game start, i.e. secondsLeft == gameDuration).
        if (progress.firstEvent) {
            progress.itemReceivedSecondsLeft = world.roundDuration();
            progress.firstEvent = false;

            // Can't know how long the very first item was held if it opens with a skip.
            if (foundEvent.isSkipped() && withinSeconds > 0) {
                progress.itemReceivedSecondsLeft = secondsLeft;
                return false;
            }
        }

        boolean result = false;

        if (foundEvent.isSkipped()) {
            if (withinSeconds > 0) {
                long timeSinceReceived = progress.itemReceivedSecondsLeft - secondsLeft;
                if (timeSinceReceived <= withinSeconds) {
                    progress.skipCount++;
                    result = progress.skipCount >= targetSkips;
                } else if (requireConsecutive) {
                    progress.skipCount = 0;
                }
            } else {
                progress.skipCount++;
                result = progress.skipCount >= targetSkips;
            }
        } else {
            // Found (not skipped) breaks a consecutive-skip streak.
            if (requireConsecutive) {
                progress.skipCount = 0;
            }
        }

        // A new item is assigned immediately after finding or skipping.
        progress.itemReceivedSecondsLeft = secondsLeft;

        return result;
    }

    @Override
    public SkipAchievementProgress createProgress() {
        return new SkipAchievementProgress();
    }
}
