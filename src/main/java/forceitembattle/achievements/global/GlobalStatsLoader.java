package forceitembattle.achievements.global;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import forceitembattle.util.Scheduler;
import forceitembattle.ForceItemBattle;
import forceitembattle.model.StatsView;
import forceitembattle.service.FibStatisticsClient;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GlobalStatsLoader {

    private final ForceItemBattle plugin;
    private final GlobalStatsCache cache;

    public GlobalStatsLoader(ForceItemBattle plugin, GlobalStatsCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }

    public void load(UUID playerUuid, Consumer<GlobalStats> onLoaded) {
        GlobalStats cached = this.cache.get(playerUuid);
        if (cached != null) {
            onLoaded.accept(cached);
            return;
        }

        FibStatisticsClient statistics = this.plugin.getFibService().statistics();

        AtomicReference<StatsView> solo = new AtomicReference<>();
        AtomicReference<StatsView> team = new AtomicReference<>();
        AtomicReference<FibPlayerStatsDto> player = new AtomicReference<>();
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

        statistics.getSoloStatisticsAsync(playerUuid,
                stats -> {
                    solo.set(StatsView.of(stats));
                    soloDone.set(true);
                    maybeDeliver.run();
                },
                error -> {
                    soloDone.set(true);
                    maybeDeliver.run();
                });

        statistics.getPlayerCombinedTeamStatsAsync(playerUuid,
                stats -> {
                    team.set(StatsView.of(stats));
                    teamDone.set(true);
                    maybeDeliver.run();
                },
                error -> {
                    teamDone.set(true);
                    maybeDeliver.run();
                });

        statistics.getPlayerStatsAsync(playerUuid,
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
