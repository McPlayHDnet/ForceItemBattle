package forceitembattle.achievements.global;

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
            GlobalStats stats = GlobalStats.of(solo.get(), team.get());
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
    }
}
