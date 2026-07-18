package forceitembattle.achievements.global;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player collection: namespaced item key -> {@link CollectedItem} (first found, times found),
 * sourced from match history with skips excluded. Pure storage, mirroring {@link GlobalStatsCache}:
 * the read-through fill lives in {@link FoundItemsLoader}, and write-invalidation is driven from the
 * match submit in FibMatchHistoryClient.
 *
 * The key set alone answers "has this player found item X", which is what the collection
 * achievement needs; the values drive the collection book's date and count display.
 */
public class FoundItemsCache {

    private final Map<UUID, Map<String, CollectedItem>> byPlayer = new ConcurrentHashMap<>();

    public Map<String, CollectedItem> get(UUID playerUuid) {
        return this.byPlayer.get(playerUuid);
    }

    public void put(UUID playerUuid, Map<String, CollectedItem> collected) {
        this.byPlayer.put(playerUuid, collected);
    }

    public void invalidate(UUID playerUuid) {
        if (playerUuid != null) {
            this.byPlayer.remove(playerUuid);
        }
    }

    public void clear() {
        this.byPlayer.clear();
    }
}
