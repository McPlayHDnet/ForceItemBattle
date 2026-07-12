package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.CounterAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class CounterAchievementHandler implements AchievementHandler<CounterAchievementProgress> {

    private final int targetAmount;
    private final boolean requireConsecutive;
    @Nullable
    private final Dimension dimension;

    public CounterAchievementHandler(int targetAmount, boolean requireConsecutive, @Nullable Dimension dimension) {
        this.targetAmount = targetAmount;
        this.requireConsecutive = requireConsecutive;
        this.dimension = dimension;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, CounterAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        if (!requireConsecutive && dimension == null) {
            progress.count++;
            return progress.count >= targetAmount;
        }

        // CONSECUTIVE achievements — tracked on the (shared, in teams) progress tracker.

        // Skip events break the streak
        if (foundEvent.isSkipped()) {
            if (requireConsecutive) {
                progress.consecutiveCount = 0;
            }
            return false;
        }

        Material itemType = foundEvent.getFoundItem().getType();

        // Check dimension if specified
        if (dimension != null && !plugin.getItemDifficultiesManager().getItemsIn(dimension).contains(itemType)) {
            if (requireConsecutive) {
                progress.consecutiveCount = 0;
            }
            return false;
        }

        // Update counters
        if (requireConsecutive) {
            progress.consecutiveCount++;
            return progress.consecutiveCount >= targetAmount;
        } else {
            progress.count++;
            return progress.count >= targetAmount;
        }
    }


    @Override
    public CounterAchievementProgress createProgress() {
        return new CounterAchievementProgress();
    }
}
