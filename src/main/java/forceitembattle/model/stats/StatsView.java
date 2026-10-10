package forceitembattle.model.stats;

import forceitembattle.model.Rarity;
import forceitembattle.model.RarityCounts;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Mapped from the generated types in {@code service/ReadModel}, so regenerating the client can't reach a renderer.
 *
 * @param memberStats per-member contributions; only a duo view has them
 */
public record StatsView(
        long gamesPlayed,
        long gamesWon,
        long totalItemsFound,
        List<ItemCount> topThreeItems,
        long blocksTravelled,
        long highestScore,
        String scoreLabel,
        long highestB2BStreak,
        RarityCounts rarities,
        long deaths,
        long longestItemStreak,
        long wheelOfFortuneUses,
        long antimatterTeleports,
        long totalTimeSpentOnItems,
        @Nullable Long teamsPlayedWith,
        List<TeamMemberStats> memberStats
) {

    public double winPercentage() {
        return gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
    }

    public double averageItemsPerGame() {
        return gamesPlayed != 0 ? (double) totalItemsFound / gamesPlayed : 0;
    }

    public double averageBackToBacksPerGame() {
        return gamesPlayed != 0 ? (double) Rarity.total(rarities) / gamesPlayed : 0;
    }

    public long averageTimePerItem() {
        return totalItemsFound > 0 ? totalTimeSpentOnItems / totalItemsFound : 0;
    }
}
