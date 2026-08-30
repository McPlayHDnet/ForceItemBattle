package forceitembattle.listener;

import forceitembattle.manager.Gamemanager;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

@RequiredArgsConstructor
public class PreGameLockListener implements Listener {
    private final Gamemanager gamemanager;
    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (this.gamemanager.roundRunning()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.gamemanager.roundRunning()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (this.gamemanager.roundRunning()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (this.gamemanager.roundRunning()) {
            return;
        }
        event.setCancelled(true);
    }
}
