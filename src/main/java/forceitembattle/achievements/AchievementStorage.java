package forceitembattle.achievements;

import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibAchievementClient;
import forceitembattle.util.Scheduler;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;

public class AchievementStorage {

    private final Plugin plugin;
    private final FIBServiceClient fibService;

    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public AchievementStorage(Plugin plugin, FIBServiceClient fibService) {
        this.plugin = plugin;
        this.fibService = fibService;
    }

    private FibAchievementClient achievementClient() {
        return this.fibService.achievements();
    }

    /**
     * Ensures the player's achievements are loaded from the service, then runs
     * {@code onLoaded} on the main thread (also runs it if already loaded). A load
     * failure is logged; {@code onLoaded} still runs so callers don't hang.
     */
    public void loadPlayer(UUID playerUUID, Runnable onLoaded) {
        if (loaded.contains(playerUUID)) {
            if (onLoaded != null) {
                Scheduler.runSync(onLoaded);
            }
            return;
        }

        achievementClient().unlockedIds(playerUUID,
                ids -> {
                    cache.computeIfAbsent(playerUUID, key -> ConcurrentHashMap.newKeySet()).addAll(ids);
                    loaded.add(playerUUID);
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                },
                error -> {
                    plugin.getLogger().warning("Failed to load achievements for " + playerUUID
                            + " (HTTP " + error.getCode() + "): " + error.getMessage());
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                });
    }


    public void unloadPlayer(UUID playerUUID) {
        cache.remove(playerUUID);
        loaded.remove(playerUUID);
    }

    public boolean isLoaded(UUID playerUUID) {
        return loaded.contains(playerUUID);
    }

    public boolean hasAchievement(UUID playerUUID, Achievements achievement) {
        Set<String> achievements = cache.get(playerUUID);
        return achievements != null && achievements.contains(achievement.name());
    }

    public Set<String> getPlayerAchievements(UUID playerUUID) {
        Set<String> achievements = cache.get(playerUUID);
        return achievements != null ? achievements : Collections.emptySet();
    }

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

        achievementClient().unlockAsync(playerUUID, achievement.name(), mode, teammateUuid);
    }

    public void removeAchievement(UUID playerUUID, Achievements achievement) {
        Set<String> achievements = cache.get(playerUUID);
        if (achievements != null) {
            achievements.remove(achievement.name());
        }
        achievementClient().removeAchievementAsync(playerUUID, achievement.name());
    }

    public void resetPlayerAchievements(UUID playerUUID) {
        cache.remove(playerUUID);
        loaded.remove(playerUUID);
        achievementClient().resetPlayerAchievementsAsync(playerUUID);
    }
}
