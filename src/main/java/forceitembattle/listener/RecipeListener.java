package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.RecipeInventory;
import forceitembattle.util.RecipeViewer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RecipeListener implements Listener {

    private ForceItemBattle forceItemBattle;

    public RecipeListener(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getServer().getPluginManager().registerEvents(this, this.forceItemBattle);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {


        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }


    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!(inventoryCloseEvent.getPlayer() instanceof Player player)) {
            return;
        }


    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        if (this.forceItemBattle.getRecipeManager().isShowingRecipe(event.getPlayer())) {
            this.forceItemBattle.getRecipeManager().handleRecipeClose(event.getPlayer());
        }
    }
}
