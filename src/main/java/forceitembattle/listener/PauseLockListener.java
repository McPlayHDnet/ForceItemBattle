package forceitembattle.listener;

import forceitembattle.model.RoundPhase;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@RequiredArgsConstructor
public class PauseLockListener implements Listener {
    private final RoundPhase roundPhase;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.roundPhase.isPausedGame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.roundPhase.isPausedGame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (this.roundPhase.isPausedGame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (this.roundPhase.isPausedGame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (this.roundPhase.isPausedGame() && event.getRemover() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
