package forceitembattle.achievements;

import de.threeseconds.openapi.fibservice.client.model.FibAchievementDto;
import forceitembattle.util.Scheduler;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementUnlockRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerAchievementsDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibAchievementClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementStorage {

    private final ForceItemBattle plugin;

    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public AchievementStorage(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    private FibAchievementClient achievementClient() {
        return plugin.getFibService().achievements();
    }

    public void loadPlayer(UUID playerUUID) {
        loadPlayer(playerUUID, null);
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

        achievementClient().getPlayerAchievementsAsync(playerUUID,
                dto -> {
                    cache.computeIfAbsent(playerUUID, key -> ConcurrentHashMap.newKeySet()).addAll(extractIds(dto));
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

    private Set<String> extractIds(FibPlayerAchievementsDto dto) {
        Set<String> ids = ConcurrentHashMap.newKeySet();
        if (dto != null && dto.getAchievements() != null) {
            for (FibAchievementDto achievement : dto.getAchievements()) {
                if (achievement.getAchievementId() != null) {
                    ids.add(achievement.getAchievementId());
                }
            }
        }
        return ids;
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

    public Map<UUID, Set<String>> getAllAchievements() {
        return new HashMap<>(cache);
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

        FibAchievementUnlockRequestDto request = FIBServiceClient.achievementUnlock()
                .mode(mode == AchievementMode.TEAM ? FibAchievementUnlockRequestDto.ModeEnum.TEAM : FibAchievementUnlockRequestDto.ModeEnum.SOLO)
                .teammateUuid(mode == AchievementMode.TEAM ? teammateUuid : null);

        achievementClient().unlockAchievementAsync(playerUUID, achievement.name(), request);
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
