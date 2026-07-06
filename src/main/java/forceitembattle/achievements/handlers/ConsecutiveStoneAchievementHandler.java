package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.MaterialCategory;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class ConsecutiveStoneAchievementHandler implements AchievementHandler<ConsecutiveStoneAchievementHandler.AchievementProgress> {

    private final int targetAmount;

    public ConsecutiveStoneAchievementHandler(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, AchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
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
    public AchievementProgress createProgress() {
        return new AchievementProgress();
    }

    public static class AchievementProgress implements AchievementProgressTracker {
        public int consecutiveCount = 0;
    }
}
