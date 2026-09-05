package forceitembattle.model.stats;

import javax.annotation.Nullable;

/**
 * One row of a leaderboard: where they placed, who they are, and the figure they placed on.
 *
 * <p>Covers three generated shapes. The achievement leaderboard's {@code count} and the statistic
 * leaderboards' {@code value} are the same column as far as every renderer is concerned — both are
 * passed to the same {@code sendRow} — so they are one field here.
 */
public record LeaderboardEntry(int rank, @Nullable PlayerIdentity player, long value) {
}
