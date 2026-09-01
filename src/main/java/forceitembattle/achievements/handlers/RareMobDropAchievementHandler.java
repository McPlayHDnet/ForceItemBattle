package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

public class RareMobDropAchievementHandler extends CountingAchievementHandler {

    public RareMobDropAchievementHandler(int targetAmount) {
        super(targetAmount);
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.MOB_DEATH;
    }

    @Override
    protected boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof EntityDeathEvent deathEvent)) {
            return false;
        }

        Material rareDrop = switch (deathEvent.getEntityType()) {
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case DROWNED -> Material.TRIDENT;
            default -> null;
        };
        if (rareDrop == null) {
            return false;
        }

        // The mob must have actually rolled the rare drop this death.
        return deathEvent.getDrops().stream().anyMatch(stack -> stack.getType() == rareDrop);
    }

    @Override
    public boolean isPlayerBased() {
        return true;
    }
}
