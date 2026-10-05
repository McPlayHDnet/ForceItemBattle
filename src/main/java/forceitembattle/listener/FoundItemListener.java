package forceitembattle.listener;

import forceitembattle.model.Roster;
import forceitembattle.manager.FoundItemResolver;
import forceitembattle.manager.Gamemanager;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.Find;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Reduces the event's ItemStack to a Material so nothing downstream needs a running server. */
@RequiredArgsConstructor
public class FoundItemListener implements Listener {
    private final Roster roster;
    private final FoundItemResolver foundItemResolver;
    private final Gamemanager gamemanager;
    @EventHandler
    public void onFoundItem(FoundItemEvent event) {
        // participant(), not get(): joker skips and back-to-back chains must not credit someone who stopped playing.
        this.roster.participant(event.getPlayer().getUniqueId())
                .ifPresent(finder -> this.foundItemResolver.resolve(Find.of(event, finder)));
    }
}
