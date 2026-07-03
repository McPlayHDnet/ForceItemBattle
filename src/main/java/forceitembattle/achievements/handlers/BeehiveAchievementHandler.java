package forceitembattle.achievements.handlers;

import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handler for beehive harvesting achievements
 */
public class BeehiveAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;

    public BeehiveAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.BEEHIVE_HARVEST;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) {
            return false;
        }

        // PlayerInteractEvent fires once per hand; only count the main hand so a
        // single right-click doesn't increment twice.
        if (interactEvent.getHand() != EquipmentSlot.HAND) {
            return false;
        }

        if (interactEvent.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        ItemStack item = interactEvent.getItem();
        if (item == null || item.getType() != Material.SHEARS) {
            return false;
        }

        Block block = interactEvent.getClickedBlock();
        if (block == null || (block.getType() != Material.BEEHIVE && block.getType() != Material.BEE_NEST)) {
            return false;
        }

        if (block.getBlockData() instanceof org.bukkit.block.data.type.Beehive beehive) {
            if (beehive.getHoneyLevel() == beehive.getMaximumHoneyLevel()) {
                progress.count++;
                return progress.count >= targetAmount;
            }
        }

        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}