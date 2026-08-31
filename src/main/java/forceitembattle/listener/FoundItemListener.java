package forceitembattle.listener;

import forceitembattle.model.Roster;
import forceitembattle.manager.FoundItemResolver;
import forceitembattle.manager.Gamemanager;
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
    private final Roster roster;
    private final FoundItemResolver foundItemResolver;
    private final Gamemanager gamemanager;
    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        ForceItemPlayer finder = this.roster
                .get(event.getPlayer().getUniqueId());

        // No roster entry means whoever fired this is not in the round -- someone who joined
        // after the countdown froze the roster. There is no score to credit, and every step of
        // the resolver reads the finder.
        if (finder == null) {
            return;
        }

        this.foundItemResolver.resolve(Find.of(event, finder));
    }
}
