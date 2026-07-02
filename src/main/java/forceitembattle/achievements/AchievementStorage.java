package forceitembattle.achievements;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementStorage {

    private static final String DEFAULT_SERVICE_URL = "http://127.0.0.7:29708";

    private final ForceItemBattle plugin;
    private final AchievementApiClient apiClient;

    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CompletableFuture<Set<String>>> inFlight = new ConcurrentHashMap<>();

    public AchievementStorage(ForceItemBattle plugin) {
        this.plugin = plugin;
        String baseUrl = plugin.getConfig().getString("achievements.service-url", DEFAULT_SERVICE_URL);
        this.apiClient = new AchievementApiClient(baseUrl, plugin.getLogger());
    }

    // ==================== LOADING ====================

    public CompletableFuture<Set<String>> loadPlayer(UUID playerUUID) {
        if (loaded.contains(playerUUID)) {
            return CompletableFuture.completedFuture(getPlayerAchievements(playerUUID));
        }

        return inFlight.computeIfAbsent(playerUUID, uuid ->
                apiClient.fetchPlayerAchievements(uuid).whenComplete((serverIds, throwable) -> {
                    if (serverIds != null) {
                        cache.computeIfAbsent(uuid, key -> ConcurrentHashMap.newKeySet()).addAll(serverIds);
                        loaded.add(uuid);
                    }
                    inFlight.remove(uuid);
                })
        );
    }

    public void loadPlayer(UUID playerUUID, Runnable onLoaded) {
        loadPlayer(playerUUID).whenComplete((ids, throwable) ->
                Bukkit.getScheduler().runTask(plugin, onLoaded));
    }

    public void unloadPlayer(UUID playerUUID) {
        cache.remove(playerUUID);
        loaded.remove(playerUUID);
    }

    public boolean isLoaded(UUID playerUUID) {
        return loaded.contains(playerUUID);
    }

    // ==================== READS (cache only) ====================

    public boolean hasAchievement(UUID playerUUID, Achievements achievement) {
        Set<String> achievements = cache.get(playerUUID);
        return achievements != null && achievements.contains(achievement.name());
    }

    public Set<String> getPlayerAchievements(UUID playerUUID) {
        Set<String> achievements = cache.get(playerUUID);
        return achievements != null ? achievements : Collections.emptySet();
    }

    public Map<UUID, Set<String>> getAllAchievements() {
        return new HashMap<>(cache);
    }

    // ==================== WRITES (cache + async service) ====================

    /**
     * Grants an achievement recorded as SOLO with no teammate. Convenience for
     * manual/admin grants that have no game context.
     */
    public void addAchievement(UUID playerUUID, Achievements achievement) {
        addAchievement(playerUUID, achievement, AchievementMode.SOLO, null);
    }

    /**
     * Grants an achievement, recording the mode and (for TEAM) the teammate on
     * the service. The cache only tracks the achievement id — mode is not part
     * of the local de-dup.
     */
    public void addAchievement(UUID playerUUID, Achievements achievement, AchievementMode mode, UUID teammateUuid) {
        cache.computeIfAbsent(playerUUID, key -> ConcurrentHashMap.newKeySet()).add(achievement.name());
        apiClient.unlockAchievement(playerUUID, achievement.name(), mode.name(), teammateUuid);
    }

    public void removeAchievement(UUID playerUUID, Achievements achievement) {
        Set<String> achievements = cache.get(playerUUID);
        if (achievements != null) {
            achievements.remove(achievement.name());
        }
        apiClient.removeAchievement(playerUUID, achievement.name());
    }

    public void resetPlayerAchievements(UUID playerUUID) {
        cache.remove(playerUUID);
        loaded.remove(playerUUID);
        apiClient.resetPlayer(playerUUID);
    }
}