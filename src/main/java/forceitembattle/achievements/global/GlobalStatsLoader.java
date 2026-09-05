package forceitembattle.achievements.global;

import forceitembattle.model.stats.GlobalPlayerStats;
import forceitembattle.model.stats.StatsView;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.util.Scheduler;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GlobalStatsLoader {

    private final FIBServiceClient fibService;
    private final GlobalStatsCache cache;

    public GlobalStatsLoader(FIBServiceClient fibService, GlobalStatsCache cache) {
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

        AtomicReference<StatsView> solo = new AtomicReference<>();
        AtomicReference<StatsView> team = new AtomicReference<>();
        AtomicReference<GlobalPlayerStats> player = new AtomicReference<>();
        AtomicBoolean playerDone = new AtomicBoolean();
        AtomicBoolean soloDone = new AtomicBoolean();
        AtomicBoolean teamDone = new AtomicBoolean();
        AtomicBoolean delivered = new AtomicBoolean();

        Runnable maybeDeliver = () -> {
            // All three sources, player-stats included: drop one from this gate and a slow call
            // delivers with its AtomicReference still null.
            if (!soloDone.get() || !teamDone.get() || !playerDone.get()) {
                return;
            }
            if (!delivered.compareAndSet(false, true)) {
                return;
            }
            GlobalStats stats = GlobalStats.of(new GlobalStatSources(solo.get(), team.get(), player.get()));
            this.cache.put(playerUuid, stats);
            Scheduler.runSync(() -> onLoaded.accept(stats));
        };

        statistics.soloStats(playerUuid,
                view -> {
                    solo.set(view);
                    soloDone.set(true);
                    maybeDeliver.run();
                },
                error -> {
                    soloDone.set(true);
                    maybeDeliver.run();
                });

        statistics.combinedTeamStats(playerUuid,
                view -> {
                    team.set(view);
                    teamDone.set(true);
                    maybeDeliver.run();
                },
                error -> {
                    teamDone.set(true);
                    maybeDeliver.run();
                });

        statistics.playerStats(playerUuid,
                stats -> {
                    player.set(stats);
                    playerDone.set(true);
                    maybeDeliver.run();
                },
                error -> {
                    playerDone.set(true);
                    maybeDeliver.run();
                });
    }
}
