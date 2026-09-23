package forceitembattle.ceremony;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

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
}
