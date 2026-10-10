package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.api.FibAchievementControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibCatalogueControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibMatchControllerApi;
import de.threeseconds.openapi.fibservice.client.api.FibStatisticsControllerApi;
import de.threeseconds.openapi.fibservice.client.invoker.ApiClient;
import forceitembattle.achievements.AchievementManager;
import forceitembattle.achievements.global.GlobalStats;
import forceitembattle.collection.CollectionManager;
import forceitembattle.manager.Manager;
import forceitembattle.util.Scheduler;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.plugin.Plugin;

public class FIBServiceClient implements Manager {

    private static final String BASE_URL = "http://127.0.0.7:29708";

    private final ApiClient apiClient;
    private final ApiExecutor executor;
    private final FibStatisticsClient statistics;
    private final StatisticsWrites statisticsWrites;
    private final FibAchievementClient achievements;
    private final FibMatchHistoryClient matchHistory;
    private final FibCatalogueClient catalogue;

    public FIBServiceClient(Plugin plugin, Map<UUID, GlobalStats> globalStatsCache,
                            Supplier<AchievementManager> achievementManager,
                            Supplier<CollectionManager> collection) {
        ApiClient client = new ApiClient();
        client.setBasePath(BASE_URL);
        this.apiClient = client;

        this.executor = new ApiExecutor(plugin);
        this.statistics = new FibStatisticsClient(new FibStatisticsControllerApi(client), executor,
                globalStatsCache);
        this.statisticsWrites = new StatisticsWrites(this.statistics);
        this.achievements = new FibAchievementClient(new FibAchievementControllerApi(client), executor);
        this.matchHistory = new FibMatchHistoryClient(new FibMatchControllerApi(client), executor,
                achievementManager, collection);
        this.catalogue = new FibCatalogueClient(new FibCatalogueControllerApi(client), executor, plugin, collection);
    }

    @Override
    public void enable() {
        Scheduler.runLaterSync(() -> this.catalogue.publishAsync(), 1L);
    }

    public FibStatisticsClient statistics() {
        return statistics;
    }

    /** The write rules. Reads go through {@link #statistics()}; nothing writes through both. */
    public StatisticsWrites statisticsWrites() {
        return statisticsWrites;
    }

    public FibAchievementClient achievements() {
        return achievements;
    }

    public FibMatchHistoryClient matchHistory() {
        return matchHistory;
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
