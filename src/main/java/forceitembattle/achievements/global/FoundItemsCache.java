package forceitembattle.achievements.global;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player set of distinct namespaced item keys the player has actually found (skips excluded),
 * sourced from match history. Pure storage, mirroring {@link GlobalStatsCache}: the read-through
 * fill lives in {@link FoundItemsLoader}, and write-invalidation is driven from the match submit
 * in FibMatchHistoryClient. Kept separate from GlobalStatsCache so each cache stays single-purpose
 * with its own value type and its own invalidation trigger (a match write, not a stat write).
 */
public class FoundItemsCache {

    private final Map<UUID, Set<String>> byPlayer = new ConcurrentHashMap<>();

    public Set<String> get(UUID playerUuid) {
        return this.byPlayer.get(playerUuid);
    }

    public void put(UUID playerUuid, Set<String> foundItems) {
        this.byPlayer.put(playerUuid, foundItems);
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
