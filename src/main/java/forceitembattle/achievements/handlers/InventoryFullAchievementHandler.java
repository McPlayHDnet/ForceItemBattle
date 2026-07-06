package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handler for full inventory achievement
 */
public class InventoryFullAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.INVENTORY_FULL;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof FoundItemEvent)) {
            return false;
        }

        Player player = forceItemPlayer.player();
        Inventory inv = player.getInventory();

        // Check all 36 slots (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
        }

        // Also check backpack if enabled
        ForceItemBattle fib = plugin;
        if (fib.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
            Inventory backpack = fib.getBackpackManager().getBackpackForPlayer(player);
            if (backpack != null) {
                for (ItemStack item : backpack.getContents()) {
                    if (item == null || item.getType() == Material.AIR) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
