package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibAchievementControllerApi;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementUnlockRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerAchievementsDto;
import java.util.UUID;
import java.util.function.Consumer;
import org.openapitools.client.ApiException;

/**
 * Achievement domain of FIBService. Wraps {@link FibAchievementControllerApi} and
 * shares transport/async plumbing via the {@link ApiExecutor} handed in by the
 * owning {@link FIBServiceClient}.
 */
public class FibAchievementClient {

    private final FibAchievementControllerApi achievementApi;
    private final ApiExecutor executor;

    FibAchievementClient(FibAchievementControllerApi achievementApi, ApiExecutor executor) {
        this.achievementApi = achievementApi;
        this.executor = executor;
    }

    public FibPlayerAchievementsDto getPlayerAchievements(UUID playerUuid) throws ApiException {
        return achievementApi.getPlayerAchievements(playerUuid);
    }

    public void getPlayerAchievementsAsync(UUID playerUuid, Consumer<FibPlayerAchievementsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> achievementApi.getPlayerAchievements(playerUuid), onSuccess, onError);
    }

    public void unlockAchievementAsync(UUID playerUuid, String achievementId, FibAchievementUnlockRequestDto request) {
        executor.runAsync(() -> achievementApi.unlockAchievement(playerUuid, achievementId, request), result -> {
        }, executor::logError);
    }

    public void removeAchievementAsync(UUID playerUuid, String achievementId) {
        executor.runAsync(() -> {
            achievementApi.removeAchievement(playerUuid, achievementId);
            return null;
        }, result -> {
        }, executor::logError);
    }

    public void resetPlayerAchievementsAsync(UUID playerUuid) {
        executor.runAsync(() -> {
            achievementApi.resetPlayerAchievements(playerUuid);
            return null;
        }, result -> {
        }, executor::logError);
    }
}
