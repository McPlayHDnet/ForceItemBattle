package forceitembattle.achievements.global;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalStatsCache {

    private final Map<UUID, GlobalStats> byPlayer = new ConcurrentHashMap<>();

    public GlobalStats get(UUID playerUuid) {
        return this.byPlayer.get(playerUuid);
    }

    public void put(UUID playerUuid, GlobalStats stats) {
        this.byPlayer.put(playerUuid, stats);
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
