package forceitembattle.listener;

import forceitembattle.manager.Gamemanager;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.gui.InventoryBuilder;
import forceitembattle.model.ForceItemPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

@RequiredArgsConstructor
public class ItemsListener implements Listener {
    private final Roster roster;
    private final RoundPhase roundPhase;
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!this.roundPhase.roundRunning()) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.roster.get(event.getPlayer().getUniqueId());
        ItemStack clickedItem = event.getItem();

        checkItemFound(event.getPlayer(), forceItemPlayer, clickedItem);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoundItemInInventory(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        if (!this.roundPhase.roundRunning()) {
            return;
        }

        if (inventoryClickEvent.getClickedInventory() instanceof CraftingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof SmithingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof BrewerInventory) {
            return;
        }

        if (inventoryClickEvent.getClickedInventory() != null
                && inventoryClickEvent.getClickedInventory().getHolder() instanceof InventoryBuilder) {
            return; //prevents from getting the needed item onClick inside any custom GUI
        }

        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        ItemStack clickedItem = inventoryClickEvent.getCurrentItem();

        checkItemFound(player, forceItemPlayer, clickedItem);
    }

    @EventHandler
    public void onPickupEvent(EntityPickupItemEvent entityPickupItemEvent) {
        if (!(entityPickupItemEvent.getEntity() instanceof Player player)) {
            return;
        }
        if (!this.roundPhase.roundRunning()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        ItemStack pickedItem = entityPickupItemEvent.getItem().getItemStack();

        checkItemFound(player, forceItemPlayer, pickedItem);
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

        if (!this.roundPhase.roundRunning()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        checkItemFound(player, forceItemPlayer, clickedItem);
    }

    @EventHandler
    public void onCrafting(CraftItemEvent craftItemEvent) {
        onCraft(craftItemEvent);
    }

    @EventHandler
    public void onSmith(SmithItemEvent smithItemEvent) {
        onCraft(smithItemEvent);
    }

    private void onCraft(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();
        if (!this.roundPhase.roundRunning()) {
            return;
        }

        boolean isValidShiftClick = inventoryClickEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && player.getInventory().firstEmpty() >= 0;

        if (isValidShiftClick || inventoryClickEvent.getAction() == InventoryAction.PICKUP_ALL) {
            ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
            ItemStack clickedItem = inventoryClickEvent.getCurrentItem();

            checkItemFound(player, forceItemPlayer, clickedItem);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent playerItemConsumeEvent) {
        Player player = playerItemConsumeEvent.getPlayer();

        if (!this.roundPhase.roundRunning()) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        ItemStack clickedItem = playerItemConsumeEvent.getItem();

        checkItemFound(player, forceItemPlayer, clickedItem);
    }

    private void checkItemFound(Player player, ForceItemPlayer forceItemPlayer, ItemStack item) {
        // Null means no roster entry, which means they arrived after the countdown froze the
        // roster: a spectator for this round, with no force item to match against. Every caller
        // here reads the roster directly, so this is the one place it has to be handled.
        if (forceItemPlayer == null || item == null) {
            return;
        }

        Material currentItem = forceItemPlayer.activeMaterial();

        if (Gamemanager.isBackpack(item)) return;

        if (item.getType() == currentItem) {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(item);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        }
    }

}
