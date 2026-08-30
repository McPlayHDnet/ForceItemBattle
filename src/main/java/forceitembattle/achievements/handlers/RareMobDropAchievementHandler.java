package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

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
        return Trigger.MOB_DEATH;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof EntityDeathEvent deathEvent)) {
            return false;
        }

        EntityType type = deathEvent.getEntityType();
        Material rareDrop;
        if (type == EntityType.WITHER_SKELETON) {
            rareDrop = Material.WITHER_SKELETON_SKULL;
        } else if (type == EntityType.DROWNED) {
            rareDrop = Material.TRIDENT;
        } else {
            return false;
        }

        // The mob must have actually rolled the rare drop this death.
        boolean dropped = deathEvent.getDrops().stream()
                .anyMatch(stack -> stack.getType() == rareDrop);
        if (!dropped) {
            return false;
        }

        progress.count++;
        return progress.count >= targetAmount;
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
