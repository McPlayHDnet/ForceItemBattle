package forceitembattle.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.achievements.Trigger;
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
public class RepeatItemHandler implements AchievementHandler<ItemFrequencyProgress> {

    private final int targetAmount;

    public RepeatItemHandler(int targetAmount) {
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
    public boolean check(Event event, ItemFrequencyProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }
        Material item = foundEvent.getFoundItem().getType();
        int newCount = progress.counts.merge(item, 1, Integer::sum);
        return newCount >= targetAmount;
    }

    @Override
    public ItemFrequencyProgress createProgress() {
        return new ItemFrequencyProgress();
    }
}