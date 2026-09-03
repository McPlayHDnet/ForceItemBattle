package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiException;
import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.model.stats.DuoLeaderboardEntry;
import forceitembattle.model.stats.GlobalPlayerStats;
import forceitembattle.model.stats.LeaderboardEntry;
import forceitembattle.model.stats.StatsView;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Statistics domain of FIBService: solo, team, member, combined, and leaderboard.
 *
 * <p>Transport only. This moves generated request types over HTTP and turns generated response types
 * into the read model; it decides nothing about which row a number belongs on. Those rules are
 * {@link StatisticsWrites}, which reaches this class through {@link StatisticsSink} and can
 * therefore be tested without a service. The split is what made them testable at all: while they
 * lived here the only stand-in for them was a mock of the class that owned them.
 *
 * <p>Nothing outside this package should be building a {@code Fib...RequestDto} - those types are
 * regenerated from the running service whenever a controller signature changes.
 */
public class FibStatisticsClient implements StatisticsSink {

    private final FibStatisticsControllerApi api;
    private final ApiExecutor executor;

    /**
     * The cache a write invalidates. Held directly rather than reached through
     * {@code AchievementManager}, which would make the achievement subsystem look like a dependency
     * of the stats transport.
     */
    private final GlobalStatsCache globalStats;

    FibStatisticsClient(FibStatisticsControllerApi api, ApiExecutor executor, GlobalStatsCache globalStats) {
        this.api = api;
        this.executor = executor;
        this.globalStats = globalStats;
    }

    // StatisticsSink. Four one-liners onto the async wrappers below: the interface exists so the
    // rules can be run without HTTP, not to add a layer here.

    @Override
    public void updateSolo(UUID playerUuid, FibSoloStatisticsUpdateRequestDto update) {
        this.updateSoloStatisticsAsync(playerUuid, update);
    }

    @Override
    public void updateTeam(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto update) {
        this.updateTeamStatisticsAsync(playerUuid, teammateUuid, update);
    }

    @Override
    public void updateMember(UUID playerUuid, UUID teammateUuid, UUID memberUuid,
                             FibTeamMemberStatsUpdateRequestDto update) {
        this.updateMemberStatisticsAsync(playerUuid, teammateUuid, memberUuid, update);
    }

    @Override
    public void recordOutcome(UUID playerUuid, FibPlayerStatsUpdateRequestDto update) {
        this.recordGameOutcomeAsync(playerUuid, update);
    }

    private void invalidateGlobal(UUID... playerUuids) {
        for (UUID playerUuid : playerUuids) {
            this.globalStats.invalidate(playerUuid);
        }
    }

    void getSoloStatisticsAsync(UUID playerUuid, Consumer<FibSoloStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getSoloStatistics(playerUuid), onSuccess, onError);
    }

    public void updateSoloStatisticsAsync(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request) {
        updateSoloStatisticsAsync(playerUuid, request, result -> {
        }, executor::logError);
    }

    public void updateSoloStatisticsAsync(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request, Consumer<FibSoloStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> api.updateSoloStatistics(playerUuid, request), onSuccess, onError);
    }

    public void deleteSoloStatisticsAsync(UUID playerUuid, Runnable onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid);
        executor.runAsync(() -> {
            api.deleteSoloStatistics(playerUuid);
            return null;
        }, result -> onSuccess.run(), onError);
    }

    void getTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, Consumer<FibTeamStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getTeamStatistics(playerUuid, teammateUuid), onSuccess, onError);
    }

    public void updateTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request) {
        updateTeamStatisticsAsync(playerUuid, teammateUuid, request, result -> {
        }, executor::logError);
    }

    public void updateTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request, Consumer<FibTeamStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid, teammateUuid);
        executor.runAsync(() -> api.updateTeamStatistics(playerUuid, teammateUuid, request), onSuccess, onError);
    }

    public void deleteAllTeamStatisticsForPlayerAsync(UUID playerUuid, Runnable onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> {
            api.deleteAllTeamStatisticsForPlayer(playerUuid);
            return null;
        }, result -> onSuccess.run(), onError);
    }

    public void updateMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request) {
        updateMemberStatisticsAsync(playerUuid, teammateUuid, memberUuid, request, result -> {
        }, executor::logError);
    }

    public void updateMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request, Consumer<FibTeamMemberStatsDto> onSuccess, Consumer<ApiException> onError) {
        invalidateGlobal(playerUuid, teammateUuid, memberUuid);
        executor.runAsync(() -> api.updateMemberStatistics(playerUuid, teammateUuid, memberUuid, request), onSuccess, onError);
    }

    void getPlayerCombinedTeamStatsAsync(UUID playerUuid, Consumer<FibPlayerCombinedTeamStatsDto> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getPlayerCombinedTeamStats(playerUuid), onSuccess, onError);
    }

    void getSoloLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getSoloLeaderboard(category, limit), onSuccess, onError);
    }

    void getTeamLeaderboardAsync(String category, int limit, Consumer<List<FibTeamLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getTeamLeaderboard(category, limit), onSuccess, onError);
    }

    void getCombinedTeamLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getCombinedTeamLeaderboard(category, limit), onSuccess, onError);
    }

    void getPlayerStatsAsync(UUID playerUuid, Consumer<FibPlayerStatsDto> onSuccess, Consumer<ApiException> onError) {
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

    // The read side, in the game's words. Everything above returns generated types and is reached
    // only from inside service/; these return the domain read model and are what every GUI and
    // command calls. A regenerated client changes the methods above and nothing else.

    public void soloStats(UUID playerUuid, Consumer<StatsView> onSuccess, Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getSoloStatistics(playerUuid),
                dto -> onSuccess.accept(ReadModel.soloStats(dto)), onError);
    }

    public void teamStats(UUID playerUuid, UUID teammateUuid, Consumer<StatsView> onSuccess,
                          Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getTeamStatistics(playerUuid, teammateUuid),
                dto -> onSuccess.accept(ReadModel.teamStats(dto)), onError);
    }

    public void combinedTeamStats(UUID playerUuid, Consumer<StatsView> onSuccess,
                                  Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getPlayerCombinedTeamStats(playerUuid),
                dto -> onSuccess.accept(ReadModel.combinedTeamStats(dto)), onError);
    }

    public void playerStats(UUID playerUuid, Consumer<GlobalPlayerStats> onSuccess,
                            Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getPlayerStats(playerUuid),
                dto -> onSuccess.accept(ReadModel.playerStats(dto)), onError);
    }

    public void soloLeaderboard(String category, int limit, Consumer<List<LeaderboardEntry>> onSuccess,
                                Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getSoloLeaderboard(category, limit),
                dtos -> onSuccess.accept(ReadModel.leaderboard(dtos)), onError);
    }

    public void combinedTeamLeaderboard(String category, int limit,
                                        Consumer<List<LeaderboardEntry>> onSuccess,
                                        Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getCombinedTeamLeaderboard(category, limit),
                dtos -> onSuccess.accept(ReadModel.leaderboard(dtos)), onError);
    }

    public void duoLeaderboard(String category, int limit,
                               Consumer<List<DuoLeaderboardEntry>> onSuccess,
                               Consumer<ApiException> onError) {
        executor.runAsync(() -> api.getTeamLeaderboard(category, limit),
                dtos -> onSuccess.accept(ReadModel.duoLeaderboard(dtos)), onError);
    }
}
