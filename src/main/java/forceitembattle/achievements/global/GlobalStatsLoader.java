package forceitembattle.achievements.global;

import forceitembattle.model.stats.GlobalPlayerStats;
import forceitembattle.model.stats.StatsView;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.util.Scheduler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GlobalStatsLoader {

    private final FIBServiceClient fibService;
    private final Map<UUID, GlobalStats> cache;

    public GlobalStatsLoader(FIBServiceClient fibService, Map<UUID, GlobalStats> cache) {
        this.fibService = fibService;
        this.cache = cache;
    }

    public void load(UUID playerUuid, Consumer<GlobalStats> onLoaded) {
        GlobalStats cached = this.cache.get(playerUuid);
        if (cached != null) {
            onLoaded.accept(cached);
            return;
        }

        FibStatisticsClient statistics = this.fibService.statistics();

        // A failed source completes with null rather than holding up the other two.
        CompletableFuture<StatsView> solo = new CompletableFuture<>();
        CompletableFuture<StatsView> team = new CompletableFuture<>();
        CompletableFuture<GlobalPlayerStats> player = new CompletableFuture<>();
        statistics.soloStats(playerUuid, solo::complete, error -> solo.complete(null));
        statistics.combinedTeamStats(playerUuid, team::complete, error -> team.complete(null));
        statistics.playerStats(playerUuid, player::complete, error -> player.complete(null));

        CompletableFuture.allOf(solo, team, player).thenRun(() -> {
            GlobalStats stats = GlobalStats.of(new GlobalStatSources(solo.join(), team.join(), player.join()));
            this.cache.put(playerUuid, stats);
            Scheduler.runSync(() -> onLoaded.accept(stats));
        });
    }
}
