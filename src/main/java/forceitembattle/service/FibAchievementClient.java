package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibAchievementControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementUnlockRequestDto;
import forceitembattle.achievements.AchievementMode;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import forceitembattle.model.stats.AchievementUnlock;
import forceitembattle.model.stats.LeaderboardEntry;
import java.util.Set;

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

    public void unlockAchievementAsync(UUID playerUuid, String achievementId, FibAchievementUnlockRequestDto request) {
        executor.runAsync(() -> achievementApi.unlockAchievement(playerUuid, achievementId, request), result -> {
        }, executor::logError);
    }

    /**
     * Unlocks an achievement in the game's own terms, so the caller does not have to know that the
     * generated request carries its own mode enum and wants the teammate only on a team unlock.
     */
    public void unlockAsync(UUID playerUuid, String achievementId, AchievementMode mode,
                            @Nullable UUID teammateUuid) {
        boolean team = mode == AchievementMode.TEAM;

        this.unlockAchievementAsync(playerUuid, achievementId, new FibAchievementUnlockRequestDto()
                .mode(team
                        ? FibAchievementUnlockRequestDto.ModeEnum.TEAM
                        : FibAchievementUnlockRequestDto.ModeEnum.SOLO)
                .teammateUuid(team ? teammateUuid : null));
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

    void getAchievementLeaderboardAsync(int limit, Consumer<List<FibAchievementLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> achievementApi.getAchievementLeaderboard(limit), onSuccess, onError);
    }

    // --- the read side, in the game's words --------------------------------------------------

    /** Every unlock this player holds, including the same achievement unlocked more than once. */
    public void unlocks(UUID playerUuid, Consumer<List<AchievementUnlock>> onSuccess,
                        Consumer<ApiException> onError) {
        executor.runAsync(() -> achievementApi.getPlayerAchievements(playerUuid),
                dto -> onSuccess.accept(ReadModel.unlocks(dto)), onError);
    }

    /** Just the ids, which is all the storage cache keeps. */
    public void unlockedIds(UUID playerUuid, Consumer<Set<String>> onSuccess,
                            Consumer<ApiException> onError) {
        executor.runAsync(() -> achievementApi.getPlayerAchievements(playerUuid),
                dto -> onSuccess.accept(ReadModel.unlockedIds(dto)), onError);
    }

    public void achievementLeaderboard(int limit, Consumer<List<LeaderboardEntry>> onSuccess,
                                       Consumer<ApiException> onError) {
        executor.runAsync(() -> achievementApi.getAchievementLeaderboard(limit),
                dtos -> onSuccess.accept(ReadModel.achievementLeaderboard(dtos)), onError);
    }
}
