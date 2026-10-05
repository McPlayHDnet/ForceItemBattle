package forceitembattle.achievements;

import forceitembattle.util.Scheduler;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The unlock cache, and the de-dup that keeps an achievement from being announced twice. */
public class AchievementStorage {

    private final AchievementSink sink;

    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public AchievementStorage(AchievementSink sink) {
        this.sink = sink;
    }

    /** {@code onLoaded} runs even on failure so callers don't hang; the player stays unloaded so the next call retries. */
    public void loadPlayer(UUID playerUUID, Runnable onLoaded) {
        if (loaded.contains(playerUUID)) {
            if (onLoaded != null) {
                Scheduler.runSync(onLoaded);
            }
            return;
        }

        sink.load(playerUUID,
                ids -> {
                    cache.computeIfAbsent(playerUUID, key -> ConcurrentHashMap.newKeySet()).addAll(ids);
                    loaded.add(playerUUID);
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                },
                () -> {
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

    public void addAchievement(UUID playerUUID, Achievements achievement) {
        addAchievement(playerUUID, achievement, AchievementMode.SOLO, null);
    }

    /** The local de-dup tracks only the achievement id, not the mode. */
    public void addAchievement(UUID playerUUID, Achievements achievement, AchievementMode mode, UUID teammateUuid) {
        cache.computeIfAbsent(playerUUID, key -> ConcurrentHashMap.newKeySet()).add(achievement.name());

        sink.unlock(playerUUID, achievement, mode, teammateUuid);
    }

    public void removeAchievement(UUID playerUUID, Achievements achievement) {
        Set<String> achievements = cache.get(playerUUID);
        if (achievements != null) {
            achievements.remove(achievement.name());
        }
        sink.remove(playerUUID, achievement);
    }

    public void resetPlayerAchievements(UUID playerUUID) {
        cache.remove(playerUUID);
        loaded.remove(playerUUID);
        sink.reset(playerUUID);
    }
}
