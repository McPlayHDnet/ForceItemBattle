package forceitembattle.ceremony;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

@RequiredArgsConstructor
public class ResultStageListener implements Listener {
    private final ResultStage stage;

    /** Sneaking would otherwise drop a player off their seat and out of the viewpoint. */
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player && event.isCancellable() && this.stage.isSeat(event.getDismounted())) {
            event.setCancelled(true);
        }
    }

    // Right-clicking air with an empty hand fires nothing, so a left click is what reliably reaches here.
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (this.stage.click(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
