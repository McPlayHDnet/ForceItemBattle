package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class CounterAchievementHandler implements AchievementHandler<CounterAchievementProgress> {

    private final int targetAmount;
    private final boolean requireConsecutive;
    private final String dimension;

    public CounterAchievementHandler(int targetAmount, boolean requireConsecutive, String dimension) {
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
        if (dimension != null && !isItemFromDimension(itemType, dimension, plugin)) {
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

    private boolean isItemFromDimension(Material itemType, String dimension, ForceItemBattle plugin) {
        var itemManager = plugin.getItemDifficultiesManager();
        return switch (dimension) {
            case "world" -> itemManager.getOverworldItems().contains(itemType);
            case "world_nether" -> itemManager.getNetherItems().contains(itemType);
            case "world_the_end" -> itemManager.getEndItems().contains(itemType);
            default -> false;
        };
    }

    @Override
    public CounterAchievementProgress createProgress() {
        return new CounterAchievementProgress();
    }
}
