package forceitembattle.achievements.global;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import forceitembattle.model.StatsView;
import org.jetbrains.annotations.Nullable;

public record GlobalStatSources(
        @Nullable StatsView solo,
        @Nullable StatsView team,
        @Nullable FibPlayerStatsDto player
) {
}
