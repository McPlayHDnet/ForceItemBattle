package forceitembattle.model.stats;

import javax.annotation.Nullable;

/** Covers the achievement leaderboard's count and the statistic leaderboards' value as one field. */
public record LeaderboardEntry(int rank, @Nullable PlayerIdentity player, long value) {
}
