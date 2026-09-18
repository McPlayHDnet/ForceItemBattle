package forceitembattle.achievements;

import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibAchievementClient;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.plugin.Plugin;

/**
 * {@link AchievementSink} over FIBService.
 *
 * <p>It holds the {@code Plugin} only to log a failed load, which is why the log lives here and not
 * in {@link AchievementStorage}: keeping it on this side is what leaves the storage headless.
 */
public class ServiceAchievementSink implements AchievementSink {

    private final Plugin plugin;
    private final FIBServiceClient fibService;

    public ServiceAchievementSink(Plugin plugin, FIBServiceClient fibService) {
        this.plugin = plugin;
        this.fibService = fibService;
    }

    private FibAchievementClient achievements() {
        return this.fibService.achievements();
    }

    @Override
    public void load(UUID playerUuid, Consumer<Set<String>> onLoaded, Runnable onFailure) {
        achievements().unlockedIds(playerUuid, onLoaded, error -> {
            this.plugin.getLogger().warning("Failed to load achievements for " + playerUuid
                    + " (HTTP " + error.getCode() + "): " + error.getMessage());
            onFailure.run();
        });
    }

    @Override
    public void unlock(UUID playerUuid, Achievements achievement, AchievementMode mode,
                       @Nullable UUID teammateUuid) {
        achievements().unlockAsync(playerUuid, achievement.name(), mode, teammateUuid);
    }

    @Override
    public void remove(UUID playerUuid, Achievements achievement) {
        achievements().removeAchievementAsync(playerUuid, achievement.name());
    }

    @Override
    public void reset(UUID playerUuid) {
        achievements().resetPlayerAchievementsAsync(playerUuid);
    }
}
