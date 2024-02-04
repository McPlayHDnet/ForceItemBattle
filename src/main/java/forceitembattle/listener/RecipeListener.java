package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.RecipeInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class RecipeListener implements Listener {

    private ForceItemBattle forceItemBattle;

    public RecipeListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getServer().getPluginManager().registerEvents(this, this.forceItemBattle);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!this.forceItemBattle.getRecipeInventory().isShowingRecipe(player)) {
            return;
        }

        event.setCancelled(true);

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (!RecipeInventory.SLOTS.contains(event.getSlot())) {
            return;
        }

        if (event.getClick().isShiftClick()) {
            ItemStack itemStack = event.getCurrentItem();
            if (itemStack == null) {
                return;
            }

            this.forceItemBattle.getRecipeInventory().showRecipe(player, itemStack);

        } else {
            player.sendMessage("§cSneak click to show recipe for this item!");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!(inventoryCloseEvent.getPlayer() instanceof Player player)) {
            return;
        }

        if (this.forceItemBattle.getRecipeInventory().isShowingRecipe(player) && !this.forceItemBattle.getRecipeInventory().ignoreInventoryClosed(player)) {
            inventoryCloseEvent.getInventory().clear();
            this.forceItemBattle.getRecipeInventory().handleRecipeClose(player);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (this.forceItemBattle.getRecipeInventory().isShowingRecipe(event.getPlayer())) {
            this.forceItemBattle.getRecipeInventory().handleRecipeClose(event.getPlayer());
        }
    }
}
