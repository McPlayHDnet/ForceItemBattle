package forceitembattle.achievements.global;

import forceitembattle.model.GlobalPlayerStats;
import forceitembattle.model.StatsView;
import org.jetbrains.annotations.Nullable;

public record GlobalStatSources(
        @Nullable StatsView solo,
        @Nullable StatsView team,
        @Nullable GlobalPlayerStats player
) {
}
