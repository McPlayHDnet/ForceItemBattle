package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibAchievementControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementUnlockRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Manager;
import org.openapitools.client.ApiClient;

public class FIBServiceClient implements Manager {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.7:29708";

    private final ApiClient apiClient;
    private final FibStatisticsClient statistics;
    private final FibAchievementClient achievements;

    public FIBServiceClient(ForceItemBattle plugin) {
        this(plugin, DEFAULT_BASE_URL);
    }

    public FIBServiceClient(ForceItemBattle plugin, String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        this.apiClient = client;

        ApiExecutor executor = new ApiExecutor(plugin);
        this.statistics = new FibStatisticsClient(new FibStatisticsControllerApi(client), executor);
        this.achievements = new FibAchievementClient(new FibAchievementControllerApi(client), executor);
    }

    public FibStatisticsClient statistics() {
        return statistics;
    }

    public FibAchievementClient achievements() {
        return achievements;
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

    public static FibAchievementUnlockRequestDto achievementUnlock() {
        return new FibAchievementUnlockRequestDto();
    }

    @Override
    public void disable() {
        // OkHttp keeps a connection pool (and dispatcher threads for any async
        // calls); shut them down so nothing lingers across a reload.
        var http = this.apiClient.getHttpClient();
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}
