package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.Find;
import forceitembattle.model.ForceItemPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Adapter: turns a Bukkit {@link FoundItemEvent} into a {@link Find} and hands it over.
 *
 * <p>Everything a find <em>means</em> — what it is worth, in what order, and to whom — lives in
 * {@code FoundItemResolver}. The only job left here is unwrapping the event, which is also the
 * point at which the {@code ItemStack} is reduced to a {@code Material} so that nothing downstream
 * needs a running server.
 */
@RequiredArgsConstructor
public class FoundItemListener implements Listener {

    public final ForceItemBattle plugin;

    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        ForceItemPlayer finder = this.plugin.getGamemanager()
                .getForceItemPlayer(event.getPlayer().getUniqueId());

        this.plugin.getFoundItemResolver().resolve(Find.of(event, finder));
    }
}
