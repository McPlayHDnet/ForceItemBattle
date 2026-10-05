package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.ItemFrequencyAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

/**
 * Unlocks when any item has been assigned N times this round, consecutively or not. Every assignment
 * counts, found or skipped; in a team the shared tracker counts the team's sequence.
 */
public class RepeatItemAchievementHandler implements AchievementHandler<ItemFrequencyAchievementProgress> {

    private final int targetAmount;

    public RepeatItemAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, ItemFrequencyAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }
        Material item = foundEvent.getFoundItem().getType();
        int newCount = progress.counts.merge(item, 1, Integer::sum);
        return newCount >= targetAmount;
    }

    @Override
    public ItemFrequencyAchievementProgress createProgress() {
        return new ItemFrequencyAchievementProgress();
    }
}
