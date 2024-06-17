package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

@RequiredArgsConstructor
public class ItemsListener implements Listener {

    public final ForceItemBattle plugin;

    @EventHandler(ignoreCancelled = true)
    public void onFoundItemInInventory(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (inventoryClickEvent.getClickedInventory() instanceof CraftingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof SmithingInventory ||
                // inventoryClickEvent.getClickedInventory() instanceof FurnaceInventory ||
                inventoryClickEvent.getClickedInventory() instanceof BrewerInventory) return;

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack clickedItem = inventoryClickEvent.getCurrentItem();
        Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

        if (clickedItem == null) {
            return;
        }

        if (inventoryClickEvent.getView().getTitle().startsWith("§8●")) {
            return; //prevents from getting the needed item onClick inside the recipe
        }

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    /* Found-/Skip Item */
    @EventHandler
    public void onPickupEvent(EntityPickupItemEvent entityPickupItemEvent) {
        if (entityPickupItemEvent.getEntity() instanceof Player player) {
            if(this.plugin.getGamemanager().isMidGame()) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                ItemStack pickedItem = entityPickupItemEvent.getItem().getItemStack();
                Material currentMaterial = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

                if (pickedItem.getType() == currentMaterial) {
                    FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                    foundItemEvent.setFoundItem(pickedItem);
                    foundItemEvent.setSkipped(false);

                    Bukkit.getPluginManager().callEvent(foundItemEvent);
                }
            }
        }
    }

    @EventHandler
    public void onBucketEvent(PlayerBucketEmptyEvent event) {
        onBucket(event.getPlayer(), event.getItemStack());
    }

    @EventHandler
    public void onBucketEvent(PlayerBucketFillEvent event) {
        onBucket(event.getPlayer(), event.getItemStack());
    }

    @EventHandler
    public void onBucketEvent(PlayerBucketEntityEvent event) {
        onBucket(event.getPlayer(), event.getEntityBucket());
    }

    private void onBucket(Player player, ItemStack clickedItem) {
        if (clickedItem == null) {
            return;
        }

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        Material currentItem = forceItemPlayer.getCurrentMaterial();

        if (clickedItem.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(clickedItem);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

    @EventHandler
    public void onCrafting(CraftItemEvent craftItemEvent) {
        onClick(craftItemEvent);
    }

    @EventHandler
    public void onSmith(SmithItemEvent smithItemEvent) {
        onClick(smithItemEvent);
    }

    private void onClick(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (inventoryClickEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || inventoryClickEvent.getAction() == InventoryAction.PICKUP_ALL) {
            ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            ItemStack clickedItem = inventoryClickEvent.getCurrentItem();
            Material currentItem = (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM) ? forceItemPlayer.currentTeam().getCurrentMaterial() : forceItemPlayer.currentMaterial());

            if (clickedItem == null) {
                return;
            }

            if (clickedItem.getType() == currentItem) {
                FoundItemEvent foundItemEvent = new FoundItemEvent(player);
                foundItemEvent.setFoundItem(clickedItem);
                foundItemEvent.setSkipped(false);

                Bukkit.getPluginManager().callEvent(foundItemEvent);
            }
        }
    }

}
