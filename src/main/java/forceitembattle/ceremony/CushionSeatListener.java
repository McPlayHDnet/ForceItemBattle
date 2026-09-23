package forceitembattle.ceremony;

import io.papermc.paper.event.entity.EntityBreakEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * A cushion is block-attached: with nothing under it, it breaks and drops itself at the next
 * physics check. The stage's cushions float by design, so they are held together here.
 */
@RequiredArgsConstructor
public class CushionSeatListener implements Listener {
    private final ResultStage stage;

    @EventHandler
    public void onBreak(EntityBreakEvent event) {
        if (this.stage.isSeat(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
