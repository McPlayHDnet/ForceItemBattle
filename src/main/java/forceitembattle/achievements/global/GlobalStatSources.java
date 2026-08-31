package forceitembattle.achievements.global;

import forceitembattle.model.stats.GlobalPlayerStats;
import forceitembattle.model.stats.StatsView;
import org.jetbrains.annotations.Nullable;

public record GlobalStatSources(
        @Nullable StatsView solo,
        @Nullable StatsView team,
        @Nullable GlobalPlayerStats player
) {
}
