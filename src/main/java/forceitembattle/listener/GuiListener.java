package forceitembattle.listener;

import forceitembattle.util.Scheduler;
import forceitembattle.manager.Gamemanager;
import forceitembattle.gui.InventoryBuilder;
import forceitembattle.gui.SettingsInventory;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack movedItem = event.getCurrentItem();

        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            if (event.getHotbarButton() >= 0) {
                movedItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            }
        }

        if (movedItem != null && !(event.getInventory().getHolder() instanceof SettingsInventory)) {
            if (Gamemanager.isBackpack(movedItem)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder) {
            inventoryBuilder.handleClick(event);
            return;
        }

    }

    @EventHandler
    public void onClose(InventoryCloseEvent inventoryCloseEvent) {
        if (!(inventoryCloseEvent.getPlayer() instanceof Player player)) {
            return;
        }

        if (inventoryCloseEvent.getInventory().getHolder() instanceof InventoryBuilder inventoryBuilder
                && inventoryBuilder.handleClose(inventoryCloseEvent)) {
            Scheduler.runSync(() -> inventoryBuilder.open(player));
        }
    }
}
