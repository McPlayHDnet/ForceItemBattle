package forceitembattle.listener;

import forceitembattle.model.Roster;
import forceitembattle.manager.FoundItemResolver;
import forceitembattle.manager.Gamemanager;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.Find;
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
        // participant(), not get(): the two other producers of this event are a joker skip and a
        // back-to-back chain, and neither should credit someone who has stopped playing. Whoever
        // is watching rather than playing has no score to credit, and every step of the resolver
        // reads the finder.
        this.roster.participant(event.getPlayer().getUniqueId())
                .ifPresent(finder -> this.foundItemResolver.resolve(Find.of(event, finder)));
    }
}
