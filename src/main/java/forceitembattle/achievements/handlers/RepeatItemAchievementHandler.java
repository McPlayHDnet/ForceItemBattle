package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

/**
 * "Get the same item N times in a single round" — unlocks when any single item
 * type has been assigned targetAmount times this round (not necessarily in a row).
 *
 * <p>Every assignment counts — found, skipped, or back-to-back — since each means
 * you had that item as your target. Team-eligible (OBTAIN_ITEM trigger), so the
 * tally is the shared team tracker: the team shares one assigned item, so this
 * counts the team's assignment sequence and is granted to both.
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
    public boolean check(Event event, ItemFrequencyAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
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
