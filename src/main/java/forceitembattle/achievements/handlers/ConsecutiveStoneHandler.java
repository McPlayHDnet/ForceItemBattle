package forceitembattle.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.MaterialCategory;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class ConsecutiveStoneHandler implements AchievementHandler<ConsecutiveStoneHandler.Progress> {

    private final int targetAmount;

    public ConsecutiveStoneHandler(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public static class Progress implements ProgressTracker {
        public int consecutiveCount = 0;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, Progress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        if (foundEvent.isSkipped()) {
            progress.consecutiveCount = 0;
            return false;
        }

        Material itemType = foundEvent.getFoundItem().getType();

        if (MaterialCategory.isStoneType(itemType)) {
            progress.consecutiveCount++;
            return progress.consecutiveCount >= targetAmount;
        } else {
            progress.consecutiveCount = 0;
            return false;
        }
    }

    @Override
    public Progress createProgress() {
        return new Progress();
    }
}