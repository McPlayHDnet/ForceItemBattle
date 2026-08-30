package forceitembattle.model;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * One stats screen, in the game's own words.
 *
 * <p>Three generated shapes reach this — solo, team and a player's combined team totals — and the
 * mapping from each lives behind the seam in {@code service/ReadModel}. Nothing about this record
 * knows the service exists, which is the point: regenerating the client cannot reach a renderer.
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
        /** Per-member contributions. Only a duo view has them; the others are empty. */
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
