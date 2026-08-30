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
import forceitembattle.achievements.global.GlobalStatsCache;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.Team;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Statistics domain of FIBService: solo, team, member, combined, and leaderboard.
 * Wraps {@link FibStatisticsControllerApi} and shares transport/async plumbing via
 * the {@link ApiExecutor} handed in by the owning {@link FIBServiceClient}.
 *
 * <p>Two layers, deliberately. The lower one takes generated request types and moves them over
 * HTTP. The upper one — everything under "the game's vocabulary" below — takes rounds, finds and
 * players, and is what the rest of the plugin calls. Nothing outside this package should be
 * building a {@code Fib...RequestDto}: those types are regenerated from the running service
 * whenever a controller signature changes, and a listener that names one is a listener that a
 * schema change can break.
 */
public class FibStatisticsClient {

    private final FibStatisticsControllerApi api;
    private final ApiExecutor executor;

    /**
     * The cache a write invalidates. Held directly rather than fetched through the plugin: this is
     * a service, and reaching from here into {@code AchievementManager} to find one field made the
     * whole achievement subsystem look like a dependency of the stats transport. It is not — the
     * only thing this needs is somewhere to say "that player's totals just changed".
     */
    private final GlobalStatsCache globalStats;

    FibStatisticsClient(FibStatisticsControllerApi api, ApiExecutor executor, GlobalStatsCache globalStats) {
        this.api = api;
        this.executor = executor;
        this.globalStats = globalStats;
    }

    // =====================================================================================
    // The game's vocabulary.
    //
    // Everything below takes rounds, finds and players and produces the generated request types
    // itself. Above this line is the transport; the game does not need to know it exists. The rules
    // about *which row* a number belongs on live here too — they are stats rules, not game rules,
    // and leaving them at the call sites is how gamesPlayed came to be counted twice once already.
    // =====================================================================================

    /** One tick of a counter attributed to the acting player: their solo row, or their member row. */
    public void recordPlayerCounter(UUID self, @Nullable ForceItemPlayer actor,
                                    PlayerCounter counter, long amount) {
        PlayerStatsWrite.record(this, self, actor,
                () -> counter.soloUpdate(amount),
                () -> counter.memberUpdate(amount));
    }

    /** A back-to-back of this rarity, attributed to the acting player. */
    public void recordRarity(UUID self, @Nullable ForceItemPlayer actor, Rarity rarity) {
        PlayerStatsWrite.record(this, self, actor,
                () -> new FibSoloStatisticsUpdateRequestDto().raritiesAdd(rarity.toRaritiesUpdate()),
                () -> new FibTeamMemberStatsUpdateRequestDto().raritiesAdd(rarity.toRaritiesUpdate()));
    }

    /**
     * One item obtained: the totals, the per-item tally, the time it took, and the streak.
     *
     * <p>The streak is the awkward one, and the awkwardness is why it lives here. In a team game the
     * peak belongs on the shared team row; in solo it rides along on the player's own update. A skip
     * breaks the streak, so it is reported on neither.
     */
    public void recordFind(ForceItemPlayer finder, boolean teamGame, String itemName,
                           boolean skipped, int itemStreak, long timeSpentMs) {
        UUID self = finder.player().getUniqueId();

        if (teamGame && !skipped) {
            finder.teammate().ifPresent(teammate -> this.updateTeamStatisticsAsync(
                    self, teammate.player().getUniqueId(),
                    new FibTeamStatisticsUpdateRequestDto().longestItemStreak(itemStreak)));
        }

        PlayerStatsWrite.record(this, self, finder,
                () -> {
                    FibSoloStatisticsUpdateRequestDto update = new FibSoloStatisticsUpdateRequestDto()
                            .totalItemsFoundAdd(1L)
                            .itemCountsAdd(Map.of(itemName, 1L));
                    if (!skipped) {
                        update.longestItemStreak(itemStreak);
                    }
                    if (timeSpentMs > 0) {
                        update.totalTimeSpentOnItemsAdd(timeSpentMs);
                    }
                    return update;
                },
                () -> {
                    FibTeamMemberStatsUpdateRequestDto update = new FibTeamMemberStatsUpdateRequestDto()
                            .totalItemsFoundAdd(1L)
                            .itemCountsAdd(Map.of(itemName, 1L));
                    if (timeSpentMs > 0) {
                        update.totalTimeSpentOnItemsAdd(timeSpentMs);
                    }
                    return update;
                });
    }

    /**
     * The peak of a back-to-back chain. Solo keeps their own; a team's peak is shared, so it is
     * written to <em>both</em> member rows rather than only the acting player's.
     */
    public void recordBackToBackPeak(ForceItemPlayer player, boolean teamGame,
                                     int ownStreak, int teamStreak) {
        UUID self = player.player().getUniqueId();

        if (!teamGame) {
            this.updateSoloStatisticsAsync(self,
                    new FibSoloStatisticsUpdateRequestDto().highestB2BStreak(ownStreak));
            return;
        }

        player.teammate().ifPresent(teammate -> {
            UUID other = teammate.player().getUniqueId();
            this.updateMemberStatisticsAsync(self, other, self,
                    new FibTeamMemberStatsUpdateRequestDto().highestB2BStreak(teamStreak));
            this.updateMemberStatisticsAsync(self, other, other,
                    new FibTeamMemberStatsUpdateRequestDto().highestB2BStreak(teamStreak));
        });
    }

    /**
     * A player started a round.
     *
     * <p>Both teammates write the same normalised team row, so a stat that counts rather than maxes
     * would be doubled if both sides sent it — only the primary writer does.
     */
    public void recordGameStarted(ForceItemPlayer player) {
        UUID self = player.player().getUniqueId();
        Team team = player.currentTeam();

        if (team == null) {
            this.updateSoloStatisticsAsync(self,
                    new FibSoloStatisticsUpdateRequestDto().gamesPlayedAdd(1));
            return;
        }

        if (team.isPrimaryWriter(player)) {
            player.teammate().ifPresent(teammate -> this.updateTeamStatisticsAsync(
                    self, teammate.player().getUniqueId(),
                    new FibTeamStatisticsUpdateRequestDto().gamesPlayedAdd(1)));
        }
    }

    /**
     * A player finished a round: how far they travelled, what they scored, and whether they won.
     *
     * <p>Three different scopes, which is why this is one method rather than three calls:
     * <ul>
     *   <li>travel is the player's own contribution, so it routes solo-or-member;</li>
     *   <li>score and the win go on whichever row owns the score — and {@code gamesWon} counts, so
     *       on a team only the primary writer sends it;</li>
     *   <li>the win/loss outcome is player-scoped and <em>everyone</em> reports it, winners and
     *       losers alike, because a loss is what resets a streak.</li>
     * </ul>
     */
    public void recordRoundFinished(ForceItemPlayer player, String playerName,
                                    int score, long blocksTravelled, boolean won) {
        UUID self = player.player().getUniqueId();
        Team team = player.currentTeam();

        PlayerStatsWrite.record(this, self, player,
                () -> {
                    FibSoloStatisticsUpdateRequestDto update = new FibSoloStatisticsUpdateRequestDto()
                            .blocksTravelledAdd(blocksTravelled)
                            .highestScore((long) score);
                    if (won) {
                        update.gamesWonAdd(1);
                    }
                    return update;
                },
                () -> new FibTeamMemberStatsUpdateRequestDto().blocksTravelledAdd(blocksTravelled));

        if (team != null) {
            player.teammate().ifPresent(teammate -> {
                FibTeamStatisticsUpdateRequestDto update = new FibTeamStatisticsUpdateRequestDto()
                        .highestScore((long) score);
                if (won && team.isPrimaryWriter(player)) {
                    update.gamesWonAdd(1);
                }
                this.updateTeamStatisticsAsync(self, teammate.player().getUniqueId(), update);
            });
        }

        this.recordGameOutcomeAsync(self, new FibPlayerStatsUpdateRequestDto()
                .outcome(won
                        ? FibPlayerStatsUpdateRequestDto.OutcomeEnum.WIN
                        : FibPlayerStatsUpdateRequestDto.OutcomeEnum.LOSS)
                .playerName(playerName));
    }

    private void invalidateGlobal(UUID... playerUuids) {
        for (UUID playerUuid : playerUuids) {
            this.globalStats.invalidate(playerUuid);
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
        this.globalStats.clear();
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
