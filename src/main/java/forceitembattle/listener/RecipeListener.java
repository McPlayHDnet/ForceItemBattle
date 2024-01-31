package forceitembattle.listener;

import forceitembattle.util.RecipeInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class RecipeListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!RecipeInventory.isShowingRecipe(player)) {
            return;
        }

        event.setCancelled(true);

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if(event.getSlot() == 10 ||
                event.getSlot() == 11 ||
                event.getSlot() == 12 ||
                event.getSlot() == 19 ||
                event.getSlot() == 20 ||
                event.getSlot() == 21 ||
                event.getSlot() == 28 ||
                event.getSlot() == 29 ||
                event.getSlot() == 30 ||
                event.getSlot() == 25 || event.getSlot() == 23) {
            if (event.getClick().isShiftClick()) {
                ItemStack itemStack = event.getCurrentItem();
                if (itemStack == null) {
                    return;
                }

                RecipeInventory.showRecipe(player, itemStack);

            } else {
                player.sendMessage("§cSneak click to show recipe for this item!");
            }

        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!(inventoryCloseEvent.getPlayer() instanceof Player player)) {
            return;
        }

        if (RecipeInventory.isShowingRecipe(player) && !RecipeInventory.ignoreInventoryClosed(player)) {
            inventoryCloseEvent.getInventory().clear();
            RecipeInventory.handleRecipeClose(player);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (RecipeInventory.isShowingRecipe(event.getPlayer())) {
            RecipeInventory.handleRecipeClose(event.getPlayer());
        }
    }
}
