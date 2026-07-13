package forceitembattle.achievements.global;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.model.StatsView;
import forceitembattle.service.FibStatisticsClient;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.bukkit.Bukkit;

public class GlobalStatsLoader {

    private final ForceItemBattle plugin;

    public GlobalStatsLoader(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    public void load(UUID playerUuid, Consumer<GlobalStats> onLoaded) {
        FibStatisticsClient statistics = this.plugin.getFibService().statistics();

        AtomicReference<StatsView> solo = new AtomicReference<>();
        AtomicReference<StatsView> team = new AtomicReference<>();
        AtomicReference<FibPlayerStatsDto> player = new AtomicReference<>();
        AtomicBoolean playerDone = new AtomicBoolean();
        AtomicBoolean soloDone = new AtomicBoolean();
        AtomicBoolean teamDone = new AtomicBoolean();
        AtomicBoolean delivered = new AtomicBoolean();

        Runnable maybeDeliver = () -> {
            if (!soloDone.get() || !teamDone.get()) {
                return;
            }
            if (!delivered.compareAndSet(false, true)) {
                return;
            }
            GlobalStats stats = GlobalStats.of(new GlobalStatSources(solo.get(), team.get(), player.get()));
            Bukkit.getScheduler().runTask(this.plugin, () -> onLoaded.accept(stats));
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
