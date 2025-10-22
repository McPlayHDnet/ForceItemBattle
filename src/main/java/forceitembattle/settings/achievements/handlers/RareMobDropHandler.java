package forceitembattle.settings.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

/**
 * Handler for rare mob drop achievements (Trident, Wither Skeleton Skull)
 */
public class RareMobDropHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;

    public RareMobDropHandler(int targetAmount) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        if (foundEvent.isSkipped()) {
            return false;
        }

        Material itemType = foundEvent.getFoundItem().getType();
        if (itemType == Material.TRIDENT || itemType == Material.WITHER_SKELETON_SKULL) {
            progress.count++;
            return progress.count >= targetAmount;
        }
        return false;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }

    @Override
    public boolean isPlayerBased() {
        return true;
    }
}