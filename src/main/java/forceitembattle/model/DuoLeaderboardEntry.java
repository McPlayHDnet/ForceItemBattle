package forceitembattle.model;

import javax.annotation.Nullable;

/** One row of the duo leaderboard, which places a pair rather than a player. */
public record DuoLeaderboardEntry(int rank, @Nullable PlayerIdentity player1,
                                  @Nullable PlayerIdentity player2, long value) {
}
