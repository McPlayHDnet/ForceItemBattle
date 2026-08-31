package forceitembattle.service;

import forceitembattle.achievements.global.GlobalStatsCache;
import de.threeseconds.openapi.fibservice.client.api.FibAchievementControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibCatalogueControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibMatchControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiClient;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Manager;
import forceitembattle.util.Scheduler;

public class FIBServiceClient implements Manager {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.7:29708";

    private final ApiClient apiClient;
    private final ApiExecutor executor;
    private final FibStatisticsClient statistics;
    private final FibAchievementClient achievements;
    private final FibMatchHistoryClient matchHistory;
    private final FibCatalogueClient catalogue;

    public FIBServiceClient(ForceItemBattle plugin, GlobalStatsCache globalStatsCache) {
        this(plugin, DEFAULT_BASE_URL, globalStatsCache);
    }

    public FIBServiceClient(ForceItemBattle plugin, String baseUrl, GlobalStatsCache globalStatsCache) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        this.apiClient = client;

        this.executor = new ApiExecutor(plugin);
        this.statistics = new FibStatisticsClient(new FibStatisticsControllerApi(client), executor,
                globalStatsCache);
        this.achievements = new FibAchievementClient(new FibAchievementControllerApi(client), executor);
        this.matchHistory = new FibMatchHistoryClient(new FibMatchControllerApi(client), executor, plugin);
        this.catalogue = new FibCatalogueClient(new FibCatalogueControllerApi(client), executor, plugin);
    }

    @Override
    public void enable() {
        Scheduler.runLaterSync(() -> this.catalogue.publishAsync(), 1L);
    }

    public FibStatisticsClient statistics() {
        return statistics;
    }

    public FibAchievementClient achievements() {
        return achievements;
    }

    public FibMatchHistoryClient matchHistory() {
        return matchHistory;
    }

    public static FibSoloStatisticsUpdateRequestDto soloUpdate() {
        return new FibSoloStatisticsUpdateRequestDto();
    }

    public static FibTeamMemberStatsUpdateRequestDto memberUpdate() {
        return new FibTeamMemberStatsUpdateRequestDto();
    }

    @Override
    public void disable() {
        // OkHttp keeps a connection pool (and dispatcher threads for any async
        // calls); shut them down so nothing lingers across a reload.
        this.executor.shutdown();
        var http = this.apiClient.getHttpClient();
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}
