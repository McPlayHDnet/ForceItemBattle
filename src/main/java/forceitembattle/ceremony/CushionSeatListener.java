package forceitembattle.ceremony;

import io.papermc.paper.event.entity.EntityBreakEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Cushions are block-attached and break when floating; the stage's cushions float, so they are held here. */
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
