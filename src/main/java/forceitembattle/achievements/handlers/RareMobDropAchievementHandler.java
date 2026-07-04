package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

/**
 * Handler for rare mob drop achievements (Trident, Wither Skeleton Skull)
 */
public class RareMobDropAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;

    public RareMobDropAchievementHandler(int targetAmount) {
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
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
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
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }

    @Override
    public boolean isPlayerBased() {
        return true;
    }
}