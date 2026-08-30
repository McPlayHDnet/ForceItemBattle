package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibAchievementControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibCatalogueControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibMatchControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiClient;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementUnlockRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Manager;
import forceitembattle.util.Scheduler;

public class FIBServiceClient implements Manager {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.7:29708";

    private final ForceItemBattle plugin;
    private final ApiClient apiClient;
    private final ApiExecutor executor;
    private final FibStatisticsClient statistics;
    private final FibAchievementClient achievements;
    private final FibMatchHistoryClient matchHistory;
    private final FibCatalogueClient catalogue;

    public FIBServiceClient(ForceItemBattle plugin) {
        this(plugin, DEFAULT_BASE_URL);
    }

    public FIBServiceClient(ForceItemBattle plugin, String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        this.plugin = plugin;
        this.apiClient = client;

        this.executor = new ApiExecutor(plugin);
        this.statistics = new FibStatisticsClient(new FibStatisticsControllerApi(client), executor,
                plugin.getAchievementManager().getGlobalStatsCache());
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

    public FibCatalogueClient catalogue() {
        return catalogue;
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

    public static FibMatchSubmitRequestDto matchSubmit() {
        return new FibMatchSubmitRequestDto();
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
