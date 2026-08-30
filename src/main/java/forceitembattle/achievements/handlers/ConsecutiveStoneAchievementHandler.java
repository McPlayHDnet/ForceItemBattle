package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.ConsecutiveStoneAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.MaterialCategory;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class ConsecutiveStoneAchievementHandler implements AchievementHandler<ConsecutiveStoneAchievementProgress> {

    private final int targetAmount;

    public ConsecutiveStoneAchievementHandler(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, ConsecutiveStoneAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
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
    public ConsecutiveStoneAchievementProgress createProgress() {
        return new ConsecutiveStoneAchievementProgress();
    }
}
