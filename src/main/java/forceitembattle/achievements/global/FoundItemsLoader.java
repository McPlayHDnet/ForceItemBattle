package forceitembattle.achievements.global;

import forceitembattle.ForceItemBattle;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Read-through for {@link FoundItemsCache}: a cache hit delivers immediately; a miss fetches the
 * player's found-item set from match history, caches it, and delivers. Single source, so there is
 * no fan-in gate (unlike GlobalStatsLoader). The client's async callbacks are already dispatched
 * on the main thread by ApiExecutor, so nothing here needs to re-schedule.
 *
 * On error we deliver an empty set but do NOT cache it: an empty found-set has real meaning
 * ("collected nothing"), so caching a transient failure would wrongly stall achievement progress
 * until the next match. Not caching lets the next load retry.
 */
public class FoundItemsLoader {

    private final ForceItemBattle plugin;
    private final FoundItemsCache cache;

    public FoundItemsLoader(ForceItemBattle plugin, FoundItemsCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }

    public void load(UUID playerUuid, Consumer<Set<String>> onLoaded) {
        Set<String> cached = this.cache.get(playerUuid);
        if (cached != null) {
            onLoaded.accept(cached);
            return;
        }

        this.plugin.getFibService().matchHistory().getFoundItemNamesAsync(playerUuid,
                names -> {
                    Set<String> found = new HashSet<>(names);
                    this.cache.put(playerUuid, found);
                    onLoaded.accept(found);
                },
                error -> onLoaded.accept(Set.of()));
    }
}
