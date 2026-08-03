package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.global.GlobalStatsCache;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Statistics domain of FIBService: solo, team, member, combined, and leaderboard.
 * Wraps {@link FibStatisticsControllerApi} and shares transport/async plumbing via
 * the {@link ApiExecutor} handed in by the owning {@link FIBServiceClient}.
 */
public class FibStatisticsClient {

    private final FibStatisticsControllerApi api;
    private final ApiExecutor executor;
    private final ForceItemBattle plugin;

    FibStatisticsClient(FibStatisticsControllerApi api, ApiExecutor executor, ForceItemBattle plugin) {
        this.api = api;
        this.executor = executor;
        this.plugin = plugin;
    }

    private void invalidateGlobal(UUID... playerUuids) {
        GlobalStatsCache cache = this.plugin.getAchievementManager().getGlobalStatsCache();
        for (UUID playerUuid : playerUuids) {
            cache.invalidate(playerUuid);
        }
    }

    public FibSoloStatisticsDto getSoloStatistics(UUID playerUuid) throws ApiException {
        return api.getSoloStatistics(playerUuid);
    }

    public void getSoloStatisticsAsync(UUID playerUuid, Consumer<FibSoloStatisticsDto> onSuccess) {
        getSoloStatisticsAsync(playerUuid, onSuccess, executor::logError);
    }

    public void getSoloStatisticsAsync(UUID playerUuid, Consumer<FibSoloStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getSoloStatistics(playerUuid), onSuccess, onError);
    }

    public FibSoloStatisticsDto updateSoloStatistics(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request) throws ApiException {
        return api.updateSoloStatistics(playerUuid, request);
    }

    public void updateSoloStatisticsAsync(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request) {
        updateSoloStatisticsAsync(playerUuid, request, result -> {
        }, executor::logError);
    }

    public void updateSoloStatisticsAsync(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request, Consumer<FibSoloStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> api.updateSoloStatistics(playerUuid, request), onSuccess, onError);
    }

    public void deleteSoloStatistics(UUID playerUuid) throws ApiException {
        api.deleteSoloStatistics(playerUuid);
    }

    public void deleteSoloStatisticsAsync(UUID playerUuid) {
        executor.runAsync(() -> {
            api.deleteSoloStatistics(playerUuid);
            return null;
        }, result -> {
        }, executor::logError);
    }

    public void deleteSoloStatisticsAsync(UUID playerUuid, Runnable onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> {
            api.deleteSoloStatistics(playerUuid);
            return null;
        }, result -> onSuccess.run(), onError);
    }

    public FibTeamStatisticsDto getTeamStatistics(UUID playerUuid, UUID teammateUuid) throws ApiException {
        return api.getTeamStatistics(playerUuid, teammateUuid);
    }

    public void getTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, Consumer<FibTeamStatisticsDto> onSuccess) {
        getTeamStatisticsAsync(playerUuid, teammateUuid, onSuccess, executor::logError);
    }

    public void getTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, Consumer<FibTeamStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getTeamStatistics(playerUuid, teammateUuid), onSuccess, onError);
    }

    public List<FibTeamStatisticsDto> getAllTeamStatisticsForPlayer(UUID playerUuid) throws ApiException {
        return api.getAllTeamStatisticsForPlayer(playerUuid);
    }

    public void getAllTeamStatisticsForPlayerAsync(UUID playerUuid, Consumer<List<FibTeamStatisticsDto>> onSuccess) {
        executor.runAsync(() -> api.getAllTeamStatisticsForPlayer(playerUuid), onSuccess, executor::logError);
    }

    public List<UUID> getTeammatesForPlayer(UUID playerUuid) throws ApiException {
        return api.getTeammatesForPlayer(playerUuid);
    }

    public void getTeammatesForPlayerAsync(UUID playerUuid, Consumer<List<UUID>> onSuccess) {
        executor.runAsync(() -> api.getTeammatesForPlayer(playerUuid), onSuccess, executor::logError);
    }

    public FibTeamStatisticsDto updateTeamStatistics(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request) throws ApiException {
        return api.updateTeamStatistics(playerUuid, teammateUuid, request);
    }

    public void updateTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request) {
        updateTeamStatisticsAsync(playerUuid, teammateUuid, request, result -> {
        }, executor::logError);
    }

    public void updateTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request, Consumer<FibTeamStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid, teammateUuid);
        executor.runAsync(() -> api.updateTeamStatistics(playerUuid, teammateUuid, request), onSuccess, onError);
    }

    public void deleteTeamStatistics(UUID playerUuid, UUID teammateUuid) throws ApiException {
        api.deleteTeamStatistics(playerUuid, teammateUuid);
    }

    public void deleteTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> {
            api.deleteTeamStatistics(playerUuid, teammateUuid);
            return null;
        }, result -> {
        }, executor::logError);
    }

    public void deleteAllTeamStatisticsForPlayer(UUID playerUuid) throws ApiException {
        api.deleteAllTeamStatisticsForPlayer(playerUuid);
    }

    public void deleteAllTeamStatisticsForPlayerAsync(UUID playerUuid) {
        this.plugin.getAchievementManager().getGlobalStatsCache().clear();
        executor.runAsync(() -> {
            api.deleteAllTeamStatisticsForPlayer(playerUuid);
            return null;
        }, result -> {
        }, executor::logError);
    }

    public void deleteAllTeamStatisticsForPlayerAsync(UUID playerUuid, Runnable onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> {
            api.deleteAllTeamStatisticsForPlayer(playerUuid);
            return null;
        }, result -> onSuccess.run(), onError);
    }

    public FibTeamMemberStatsDto getMemberStatistics(UUID playerUuid, UUID teammateUuid, UUID memberUuid) throws ApiException {
        return api.getMemberStatistics(playerUuid, teammateUuid, memberUuid);
    }

    public void getMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, Consumer<FibTeamMemberStatsDto> onSuccess) {
        executor.runAsync(() -> api.getMemberStatistics(playerUuid, teammateUuid, memberUuid), onSuccess, executor::logError);
    }

    public FibTeamMemberStatsDto updateMemberStatistics(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request) throws ApiException {
        return api.updateMemberStatistics(playerUuid, teammateUuid, memberUuid, request);
    }

    public void updateMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request) {
        updateMemberStatisticsAsync(playerUuid, teammateUuid, memberUuid, request, result -> {
        }, executor::logError);
    }

    public void updateMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request, Consumer<FibTeamMemberStatsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid, teammateUuid, memberUuid);
        executor.runAsync(() -> api.updateMemberStatistics(playerUuid, teammateUuid, memberUuid, request), onSuccess, onError);
    }

    public FibPlayerCombinedTeamStatsDto getPlayerCombinedTeamStats(UUID playerUuid) throws ApiException {
        return api.getPlayerCombinedTeamStats(playerUuid);
    }

    public void getPlayerCombinedTeamStatsAsync(UUID playerUuid, Consumer<FibPlayerCombinedTeamStatsDto> onSuccess) {
        getPlayerCombinedTeamStatsAsync(playerUuid, onSuccess, executor::logError);
    }

    public void getPlayerCombinedTeamStatsAsync(UUID playerUuid, Consumer<FibPlayerCombinedTeamStatsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getPlayerCombinedTeamStats(playerUuid), onSuccess, onError);
    }

    public List<FibLeaderboardEntryDto> getSoloLeaderboard(String category, int limit) throws ApiException {
        return api.getSoloLeaderboard(category, limit);
    }

    public void getSoloLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess) {
        getSoloLeaderboardAsync(category, limit, onSuccess, executor::logError);
    }

    public void getSoloLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getSoloLeaderboard(category, limit), onSuccess, onError);
    }

    public List<FibTeamLeaderboardEntryDto> getTeamLeaderboard(String category, int limit) throws ApiException {
        return api.getTeamLeaderboard(category, limit);
    }

    public void getTeamLeaderboardAsync(String category, int limit, Consumer<List<FibTeamLeaderboardEntryDto>> onSuccess) {
        getTeamLeaderboardAsync(category, limit, onSuccess, executor::logError);
    }

    public void getTeamLeaderboardAsync(String category, int limit, Consumer<List<FibTeamLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getTeamLeaderboard(category, limit), onSuccess, onError);
    }

    public List<FibLeaderboardEntryDto> getCombinedTeamLeaderboard(String category, int limit) throws ApiException {
        return api.getCombinedTeamLeaderboard(category, limit);
    }

    public void getCombinedTeamLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess) {
        getCombinedTeamLeaderboardAsync(category, limit, onSuccess, executor::logError);
    }

    public void getCombinedTeamLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getCombinedTeamLeaderboard(category, limit), onSuccess, onError);
    }

    public FibPlayerStatsDto getPlayerStats(UUID playerUuid) throws ApiException {
        return api.getPlayerStats(playerUuid);
    }

    public void getPlayerStatsAsync(UUID playerUuid, Consumer<FibPlayerStatsDto> onSuccess) {
        getPlayerStatsAsync(playerUuid, onSuccess, executor::logError);
    }

    public void getPlayerStatsAsync(UUID playerUuid, Consumer<FibPlayerStatsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getPlayerStats(playerUuid), onSuccess, onError);
    }

    public void recordGameOutcomeAsync(UUID playerUuid, FibPlayerStatsUpdateRequestDto request) {
        recordGameOutcomeAsync(playerUuid, request, result -> {
        }, executor::logError);
    }

    public void recordGameOutcomeAsync(UUID playerUuid, FibPlayerStatsUpdateRequestDto request, Consumer<FibPlayerStatsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> api.recordGameOutcome(playerUuid, request), onSuccess, onError);
    }

    public void deletePlayerStatsAsync(UUID playerUuid, Runnable onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> {
            api.deletePlayerStats(playerUuid);
            return null;
        }, result -> onSuccess.run(), onError);
    }
}
