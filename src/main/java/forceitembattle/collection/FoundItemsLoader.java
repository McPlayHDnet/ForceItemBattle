package forceitembattle.collection;

import de.threeseconds.openapi.fibservice.client.model.FibFoundItemStatsDto;
import forceitembattle.ForceItemBattle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Read-through for {@link FoundItemsCache}: a cache hit delivers immediately; a miss fetches the
 * player's collection from match history, converts the generated DTOs into {@link CollectedItem},
 * caches it, and delivers. Single source, so there is no fan-in gate (unlike GlobalStatsLoader).
 * The client's async callbacks are already dispatched on the main thread by ApiExecutor.
 *
 * On error we deliver an empty map but do NOT cache it: an empty collection has real meaning
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

    public void load(UUID playerUuid, Consumer<Map<String, CollectedItem>> onLoaded) {
        Map<String, CollectedItem> cached = this.cache.get(playerUuid);
        if (cached != null) {
            onLoaded.accept(cached);
            return;
        }

        this.plugin.getFibService().matchHistory().getFoundItemStatsAsync(playerUuid,
                stats -> {
                    Map<String, CollectedItem> collected = convert(stats);
                    this.cache.put(playerUuid, collected);
                    onLoaded.accept(collected);
                },
                error -> onLoaded.accept(Map.of()));
    }

    private static Map<String, CollectedItem> convert(List<FibFoundItemStatsDto> stats) {
        Map<String, CollectedItem> collected = new HashMap<>();
        for (FibFoundItemStatsDto entry : stats) {
            collected.put(entry.getItemName(), new CollectedItem(
                    entry.getFirstCollected() != null ? entry.getFirstCollected().toInstant() : null,
                    entry.getTimesCollected() != null ? entry.getTimesCollected() : 0L));
        }
        return collected;
    }
}
