package forceitembattle.model;

import de.threeseconds.openapi.fibservice.client.model.FibItemCountDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public record StatsView(
        long gamesPlayed,
        long gamesWon,
        long totalItemsFound,
        List<FibItemCountDto> topThreeItems,
        long blocksTravelled,
        long highestScore,
        String scoreLabel,
        long highestB2BStreak,
        FibRaritiesDto rarities,
        long deaths,
        long longestItemStreak,
        long wheelOfFortuneUses,
        long antimatterTeleports,
        long totalTimeSpentOnItems,
        @Nullable Long teamsPlayedWith
) {

    public static StatsView of(FibSoloStatisticsDto stats) {
        return new StatsView(
                stats.getGamesPlayed(), stats.getGamesWon(),
                stats.getTotalItemsFound(), stats.getTopThreeItems(),
                stats.getBlocksTravelled(),
                stats.getHighestScore(), "Highest score",
                stats.getHighestB2BStreak(), stats.getRarities(),
                stats.getDeaths(), stats.getLongestItemStreak(),
                stats.getWheelOfFortuneUses(), stats.getEnteredAntimatterTeleporter(),
                stats.getTotalTimeSpentOnItems(),
                null
        );
    }

    /**
     * The team-mode view.
     *
     * <p>Rarities come from {@code getTeamRarities()}, not {@code getRarities()}: a back-to-back in a
     * team game belongs to the team. A teammate's inventory can be what makes one happen at all, and
     * the b2b streak has always been recorded for both members — but the per-rarity tally used to be
     * keyed to whoever held the item, so the global rarity achievements filled up for one member and
     * stalled for the other. Visible on RNGesus (2) and Extraordinary (5) and invisible on the rest
     * purely because their thresholds are 250 / 100 / 50. The player's own contribution is still on
     * the DTO as {@code getRarities()} if a caller ever wants to split the team's total back apart.
     */
    public static StatsView of(FibPlayerCombinedTeamStatsDto stats) {
        return new StatsView(
                stats.getTotalGamesPlayed(), stats.getTotalGamesWon(),
                stats.getTotalItemsFound(), stats.getTopThreeItems(),
                stats.getBlocksTravelled(),
                stats.getHighestTeamScore(), "Highest team score",
                stats.getHighestB2BStreak(), stats.getTeamRarities(),
                stats.getDeaths(), stats.getLongestTeamItemStreak(),
                stats.getWheelOfFortuneUses(), stats.getEnteredAntimatterTeleporter(),
                stats.getTotalTimeSpentOnItems(),
                (long) stats.getTeamsCount()
        );
    }

    public static StatsView of(FibTeamStatisticsDto stats) {
        return new StatsView(
                stats.getGamesPlayed(), stats.getGamesWon(),
                stats.getTotalItemsFound(), stats.getTopThreeItems(),
                stats.getBlocksTravelled(),
                stats.getHighestScore(), "Highest score",
                stats.getHighestB2BStreak(), stats.getRarities(),
                stats.getDeaths(), stats.getLongestItemStreak(),
                stats.getWheelOfFortuneUses(), stats.getEnteredAntimatterTeleporter(),
                stats.getTotalTimeSpentOnItems(),
                null
        );
    }

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
