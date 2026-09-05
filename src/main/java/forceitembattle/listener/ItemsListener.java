package forceitembattle.listener;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.gui.InventoryBuilder;
import forceitembattle.model.FindDetection;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
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

/**
 * The eight ways an item can reach a player's hands.
 *
 * <p>Adapter only. Each handler decides whether <em>this kind of event</em> is one that counts —
 * a right-click rather than a left, a real craft rather than a preview, an inventory that is not a
 * menu — and then asks {@link FindDetection} whether what it is holding is a find. Whether the
 * round is running, whether this player is playing it and whether the item matches is one decision
 * in one place; it used to be three lines repeated eight times here, and it was where the bugs were.
 */
@RequiredArgsConstructor
public class ItemsListener implements Listener {

    private final FindDetection detection;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        checkItemFound(event.getPlayer(), event.getItem());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoundItemInInventory(InventoryClickEvent inventoryClickEvent) {
        Player player = (Player) inventoryClickEvent.getWhoClicked();

        // The crafting, smithing and brewing grids are handled by onCraft, which has its own rule
        // about which click actually hands the result over.
        if (inventoryClickEvent.getClickedInventory() instanceof CraftingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof SmithingInventory ||
                inventoryClickEvent.getClickedInventory() instanceof BrewerInventory) {
            return;
        }

        if (inventoryClickEvent.getClickedInventory() != null
                && inventoryClickEvent.getClickedInventory().getHolder() instanceof InventoryBuilder) {
            return; //prevents from getting the needed item onClick inside any custom GUI
        }

        checkItemFound(player, inventoryClickEvent.getCurrentItem());
    }

    @EventHandler
    public void onPickupEvent(EntityPickupItemEvent entityPickupItemEvent) {
        if (!(entityPickupItemEvent.getEntity() instanceof Player player)) {
            return;
        }

        checkItemFound(player, entityPickupItemEvent.getItem().getItemStack());
    }

    @EventHandler
    public void onBucketEvent(PlayerBucketEmptyEvent event) {
        checkItemFound(event.getPlayer(), event.getItemStack());
    }

    @EventHandler
    public void onBucketEvent(PlayerBucketFillEvent event) {
        checkItemFound(event.getPlayer(), event.getItemStack());
    }

    @EventHandler
    public void onBucketEvent(PlayerBucketEntityEvent event) {
        checkItemFound(event.getPlayer(), event.getEntityBucket());
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

        boolean isValidShiftClick = inventoryClickEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && player.getInventory().firstEmpty() >= 0;

        if (isValidShiftClick || inventoryClickEvent.getAction() == InventoryAction.PICKUP_ALL) {
            checkItemFound(player, inventoryClickEvent.getCurrentItem());
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent playerItemConsumeEvent) {
        checkItemFound(playerItemConsumeEvent.getPlayer(), playerItemConsumeEvent.getItem());
    }

    private void checkItemFound(Player player, ItemStack item) {
        this.detection.detect(player, item).ifPresent(finder -> {
            FoundItemEvent foundItemEvent = new FoundItemEvent(player);
            foundItemEvent.setFoundItem(item);
            foundItemEvent.setSkipped(false);

            Bukkit.getPluginManager().callEvent(foundItemEvent);
        });
    }
}
