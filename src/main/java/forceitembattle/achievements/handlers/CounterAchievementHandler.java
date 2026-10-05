package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.CounterAchievementProgress;
import forceitembattle.collection.MaterialCategory;
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
    private final ItemFilter filter;

    public CounterAchievementHandler(int targetAmount, boolean requireConsecutive, @Nullable Dimension dimension) {
        this(dimension == null ? null : (world, material) -> world.itemsIn(dimension).contains(material),
                targetAmount, requireConsecutive);
    }

    private CounterAchievementHandler(@Nullable ItemFilter filter, int targetAmount, boolean requireConsecutive) {
        this.targetAmount = targetAmount;
        this.requireConsecutive = requireConsecutive;
        this.filter = filter;
    }

    public static CounterAchievementHandler stoneRun(int targetAmount) {
        return new CounterAchievementHandler((world, material) -> MaterialCategory.isStoneType(material),
                targetAmount, true);
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM;
    }

    @Override
    public boolean check(Event event, CounterAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        if (!requireConsecutive && filter == null) {
            progress.count++;
            return progress.count >= targetAmount;
        }

        // Consecutive achievements are tracked on the (shared, in teams) progress tracker.
        if (foundEvent.isSkipped()) {
            if (requireConsecutive) {
                progress.consecutiveCount = 0;
            }
            return false;
        }

        Material itemType = foundEvent.getFoundItem().getType();

        if (filter != null && !filter.accepts(world, itemType)) {
            if (requireConsecutive) {
                progress.consecutiveCount = 0;
            }
            return false;
        }

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

    @FunctionalInterface
    private interface ItemFilter {
        boolean accepts(AchievementWorld world, Material material);
    }
}
