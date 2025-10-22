package forceitembattle.settings.achievements.handlers;

import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handler for beehive harvesting achievements
 */
public class BeehiveHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;

    public BeehiveHandler(int targetAmount) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) {
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
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}