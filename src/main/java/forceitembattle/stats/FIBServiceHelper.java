package forceitembattle.stats;

import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class FIBServiceHelper {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.7:29708";

    private final FibStatisticsControllerApi api;
    private final ForceItemBattle plugin;

    public FIBServiceHelper(ForceItemBattle plugin) {
        this(plugin, DEFAULT_BASE_URL);
    }

    public FIBServiceHelper(ForceItemBattle plugin, String baseUrl) {
        this.plugin = plugin;
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        this.api = new FibStatisticsControllerApi(client);
    }

    public FibSoloStatisticsDto getSoloStatistics(UUID playerUuid) throws ApiException {
        return api.getSoloStatistics(playerUuid);
    }

    public void getSoloStatisticsAsync(UUID playerUuid, Consumer<FibSoloStatisticsDto> onSuccess) {
        getSoloStatisticsAsync(playerUuid, onSuccess, this::logError);
    }

    public void getSoloStatisticsAsync(UUID playerUuid, Consumer<FibSoloStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.getSoloStatistics(playerUuid), onSuccess, onError);
    }

    public FibSoloStatisticsDto updateSoloStatistics(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request) throws ApiException {
        return api.updateSoloStatistics(playerUuid, request);
    }

    public void updateSoloStatisticsAsync(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request) {
        updateSoloStatisticsAsync(playerUuid, request, result -> {}, this::logError);
    }

    public void updateSoloStatisticsAsync(UUID playerUuid, FibSoloStatisticsUpdateRequestDto request, Consumer<FibSoloStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.updateSoloStatistics(playerUuid, request), onSuccess, onError);
    }

    public void deleteSoloStatistics(UUID playerUuid) throws ApiException {
        api.deleteSoloStatistics(playerUuid);
    }

    public void deleteSoloStatisticsAsync(UUID playerUuid) {
        runAsync(() -> { api.deleteSoloStatistics(playerUuid); return null; }, result -> {}, this::logError);
    }

    public FibTeamStatisticsDto getTeamStatistics(UUID playerUuid, UUID teammateUuid) throws ApiException {
        return api.getTeamStatistics(playerUuid, teammateUuid);
    }

    public void getTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, Consumer<FibTeamStatisticsDto> onSuccess) {
        getTeamStatisticsAsync(playerUuid, teammateUuid, onSuccess, this::logError);
    }

    public void getTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, Consumer<FibTeamStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.getTeamStatistics(playerUuid, teammateUuid), onSuccess, onError);
    }

    public List<FibTeamStatisticsDto> getAllTeamStatisticsForPlayer(UUID playerUuid) throws ApiException {
        return api.getAllTeamStatisticsForPlayer(playerUuid);
    }

    public void getAllTeamStatisticsForPlayerAsync(UUID playerUuid, Consumer<List<FibTeamStatisticsDto>> onSuccess) {
        runAsync(() -> api.getAllTeamStatisticsForPlayer(playerUuid), onSuccess, this::logError);
    }

    public List<UUID> getTeammatesForPlayer(UUID playerUuid) throws ApiException {
        return api.getTeammatesForPlayer(playerUuid);
    }

    public void getTeammatesForPlayerAsync(UUID playerUuid, Consumer<List<UUID>> onSuccess) {
        runAsync(() -> api.getTeammatesForPlayer(playerUuid), onSuccess, this::logError);
    }

    public FibTeamStatisticsDto updateTeamStatistics(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request) throws ApiException {
        return api.updateTeamStatistics(playerUuid, teammateUuid, request);
    }

    public void updateTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request) {
        updateTeamStatisticsAsync(playerUuid, teammateUuid, request, result -> {}, this::logError);
    }

    public void updateTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto request, Consumer<FibTeamStatisticsDto> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.updateTeamStatistics(playerUuid, teammateUuid, request), onSuccess, onError);
    }

    public void deleteTeamStatistics(UUID playerUuid, UUID teammateUuid) throws ApiException {
        api.deleteTeamStatistics(playerUuid, teammateUuid);
    }

    public void deleteTeamStatisticsAsync(UUID playerUuid, UUID teammateUuid) {
        runAsync(() -> { api.deleteTeamStatistics(playerUuid, teammateUuid); return null; }, result -> {}, this::logError);
    }

    public void deleteAllTeamStatisticsForPlayer(UUID playerUuid) throws ApiException {
        api.deleteAllTeamStatisticsForPlayer(playerUuid);
    }

    public void deleteAllTeamStatisticsForPlayerAsync(UUID playerUuid) {
        runAsync(() -> { api.deleteAllTeamStatisticsForPlayer(playerUuid); return null; }, result -> {}, this::logError);
    }

    public FibTeamMemberStatsDto getMemberStatistics(UUID playerUuid, UUID teammateUuid, UUID memberUuid) throws ApiException {
        return api.getMemberStatistics(playerUuid, teammateUuid, memberUuid);
    }

    public void getMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, Consumer<FibTeamMemberStatsDto> onSuccess) {
        runAsync(() -> api.getMemberStatistics(playerUuid, teammateUuid, memberUuid), onSuccess, this::logError);
    }

    public FibTeamMemberStatsDto updateMemberStatistics(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request) throws ApiException {
        return api.updateMemberStatistics(playerUuid, teammateUuid, memberUuid, request);
    }

    public void updateMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request) {
        updateMemberStatisticsAsync(playerUuid, teammateUuid, memberUuid, request, result -> {}, this::logError);
    }

    public void updateMemberStatisticsAsync(UUID playerUuid, UUID teammateUuid, UUID memberUuid, FibTeamMemberStatsUpdateRequestDto request, Consumer<FibTeamMemberStatsDto> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.updateMemberStatistics(playerUuid, teammateUuid, memberUuid, request), onSuccess, onError);
    }

    public FibPlayerCombinedTeamStatsDto getPlayerCombinedTeamStats(UUID playerUuid) throws ApiException {
        return api.getPlayerCombinedTeamStats(playerUuid);
    }

    public void getPlayerCombinedTeamStatsAsync(UUID playerUuid, Consumer<FibPlayerCombinedTeamStatsDto> onSuccess) {
        getPlayerCombinedTeamStatsAsync(playerUuid, onSuccess, this::logError);
    }

    public void getPlayerCombinedTeamStatsAsync(UUID playerUuid, Consumer<FibPlayerCombinedTeamStatsDto> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.getPlayerCombinedTeamStats(playerUuid), onSuccess, onError);
    }

    public List<FibLeaderboardEntryDto> getSoloLeaderboard(String category, int limit) throws ApiException {
        return api.getSoloLeaderboard(category, limit);
    }

    public void getSoloLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess) {
        getSoloLeaderboardAsync(category, limit, onSuccess, this::logError);
    }

    public void getSoloLeaderboardAsync(String category, int limit, Consumer<List<FibLeaderboardEntryDto>> onSuccess, Consumer<ApiException> onError) {
        runAsync(() -> api.getSoloLeaderboard(category, limit), onSuccess, onError);
    }

    public static FibSoloStatisticsUpdateRequestDto soloUpdate() {
        return new FibSoloStatisticsUpdateRequestDto();
    }

    public static FibTeamStatisticsUpdateRequestDto teamUpdate() {
        return new FibTeamStatisticsUpdateRequestDto();
    }

    public static FibTeamMemberStatsUpdateRequestDto memberUpdate() {
        return new FibTeamMemberStatsUpdateRequestDto();
    }

    public static FibRaritiesUpdateRequestDto raritiesUpdate() {
        return new FibRaritiesUpdateRequestDto();
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute() throws ApiException;
    }

    private <T> void runAsync(ApiCall<T> apiCall, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T result = apiCall.execute();
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(result));
            } catch (ApiException e) {
                Bukkit.getScheduler().runTask(plugin, () -> onError.accept(e));
            }
        });
    }

    private void logError(ApiException e) {
        plugin.getLogger().log(Level.SEVERE, "[FIBService] API call failed (HTTP " + e.getCode() + "): " + e.getMessage(), e);
    }
}