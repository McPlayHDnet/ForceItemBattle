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

public class InventoryFullAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.INVENTORY_FULL;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        Player player = forceItemPlayer.player();
        Inventory inv = player.getInventory();

        // The 36 storage slots; armour and offhand don't count.
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }
        }

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
