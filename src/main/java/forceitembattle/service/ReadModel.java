package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibAchievementDto;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibCollectionRarityDto;
import de.threeseconds.openapi.fibservice.client.model.FibFoundItemStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibItemCountDto;
import de.threeseconds.openapi.fibservice.client.model.FibItemRarityDto;
import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerAchievementsDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerIdentityDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import forceitembattle.collection.CollectedItem;
import forceitembattle.collection.ItemRarity;
import forceitembattle.model.stats.AchievementUnlock;
import forceitembattle.model.stats.DuoLeaderboardEntry;
import forceitembattle.model.stats.GlobalPlayerStats;
import forceitembattle.model.stats.ItemCount;
import forceitembattle.model.stats.LeaderboardEntry;
import forceitembattle.model.stats.PlayerIdentity;
import forceitembattle.model.RarityCounts;
import forceitembattle.model.stats.StatsView;
import forceitembattle.model.stats.TeamMemberStats;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * The one place that speaks both the vendor's vocabulary and the game's, so a regenerated client
 * cannot reach into GUIs, commands and the achievement package.
 *
 * <p>Everything here is a translation and nothing here is a rule. Null-tolerance is the exception:
 * every generated field is boxed and can arrive null, and deciding that an absent count is zero is
 * a translation decision, made once here rather than at forty call sites.
 */
final class ReadModel {

    private ReadModel() {
    }

    private static long value(@Nullable Long boxed) {
        return boxed != null ? boxed : 0L;
    }

    private static int value(@Nullable Integer boxed) {
        return boxed != null ? boxed : 0;
    }

    @Nullable
    static PlayerIdentity identity(@Nullable FibPlayerIdentityDto dto) {
        return dto == null ? null : new PlayerIdentity(dto.getUuid(), dto.getName());
    }

    static RarityCounts rarities(@Nullable FibRaritiesDto dto) {
        if (dto == null) {
            return RarityCounts.NONE;
        }
        return new RarityCounts(
                value(dto.getRare()), value(dto.getEpic()), value(dto.getLegendary()),
                value(dto.getRngesus()), value(dto.getExtraordinary()));
    }

    static List<ItemCount> itemCounts(@Nullable List<FibItemCountDto> dtos) {
        List<ItemCount> items = new ArrayList<>();
        if (dtos != null) {
            for (FibItemCountDto dto : dtos) {
                items.add(new ItemCount(dto.getItemName(), value(dto.getCount())));
            }
        }
        return List.copyOf(items);
    }

    static List<TeamMemberStats> memberStats(@Nullable List<FibTeamMemberStatsDto> dtos) {
        List<TeamMemberStats> members = new ArrayList<>();
        if (dtos != null) {
            for (FibTeamMemberStatsDto dto : dtos) {
                members.add(new TeamMemberStats(identity(dto.getMember()),
                        value(dto.getTotalItemsFound()), value(dto.getDeaths()),
                        value(dto.getBlocksTravelled())));
            }
        }
        return List.copyOf(members);
    }

    static GlobalPlayerStats playerStats(@Nullable FibPlayerStatsDto dto) {
        return dto == null
                ? new GlobalPlayerStats(0)
                : new GlobalPlayerStats(value(dto.getHighestWinStreak()));
    }

    // --- stats views -------------------------------------------------------------------------

    static StatsView soloStats(FibSoloStatisticsDto stats) {
        return new StatsView(
                value(stats.getGamesPlayed()), value(stats.getGamesWon()),
                value(stats.getTotalItemsFound()), itemCounts(stats.getTopThreeItems()),
                value(stats.getBlocksTravelled()),
                value(stats.getHighestScore()), "Highest score",
                value(stats.getHighestB2BStreak()), rarities(stats.getRarities()),
                value(stats.getDeaths()), value(stats.getLongestItemStreak()),
                value(stats.getWheelOfFortuneUses()), value(stats.getEnteredAntimatterTeleporter()),
                value(stats.getTotalTimeSpentOnItems()),
                null, List.of());
    }

    /**
     * Rarities come from {@code getTeamRarities()}, not {@code getRarities()}: a back-to-back in a
     * team game belongs to the team. Keying the per-rarity tally to whoever held the item makes the
     * global rarity achievements fill up for one member and stall for the other.
     */
    static StatsView combinedTeamStats(FibPlayerCombinedTeamStatsDto stats) {
        return new StatsView(
                value(stats.getTotalGamesPlayed()), value(stats.getTotalGamesWon()),
                value(stats.getTotalItemsFound()), itemCounts(stats.getTopThreeItems()),
                value(stats.getBlocksTravelled()),
                value(stats.getHighestTeamScore()), "Highest team score",
                value(stats.getHighestB2BStreak()), rarities(stats.getTeamRarities()),
                value(stats.getDeaths()), value(stats.getLongestTeamItemStreak()),
                value(stats.getWheelOfFortuneUses()), value(stats.getEnteredAntimatterTeleporter()),
                value(stats.getTotalTimeSpentOnItems()),
                (long) value(stats.getTeamsCount()), List.of());
    }

    static StatsView teamStats(FibTeamStatisticsDto stats) {
        return new StatsView(
                value(stats.getGamesPlayed()), value(stats.getGamesWon()),
                value(stats.getTotalItemsFound()), itemCounts(stats.getTopThreeItems()),
                value(stats.getBlocksTravelled()),
                value(stats.getHighestScore()), "Highest score",
                value(stats.getHighestB2BStreak()), rarities(stats.getRarities()),
                value(stats.getDeaths()), value(stats.getLongestItemStreak()),
                value(stats.getWheelOfFortuneUses()), value(stats.getEnteredAntimatterTeleporter()),
                value(stats.getTotalTimeSpentOnItems()),
                null, memberStats(stats.getMemberStats()));
    }

    // --- leaderboards ------------------------------------------------------------------------

    static List<LeaderboardEntry> leaderboard(@Nullable List<FibLeaderboardEntryDto> dtos) {
        List<LeaderboardEntry> rows = new ArrayList<>();
        if (dtos != null) {
            for (FibLeaderboardEntryDto dto : dtos) {
                rows.add(new LeaderboardEntry(value(dto.getRank()), identity(dto.getPlayer()),
                        value(dto.getValue())));
            }
        }
        return List.copyOf(rows);
    }

    static List<DuoLeaderboardEntry> duoLeaderboard(@Nullable List<FibTeamLeaderboardEntryDto> dtos) {
        List<DuoLeaderboardEntry> rows = new ArrayList<>();
        if (dtos != null) {
            for (FibTeamLeaderboardEntryDto dto : dtos) {
                rows.add(new DuoLeaderboardEntry(value(dto.getRank()), identity(dto.getPlayer1()),
                        identity(dto.getPlayer2()), value(dto.getValue())));
            }
        }
        return List.copyOf(rows);
    }

    static List<LeaderboardEntry> achievementLeaderboard(
            @Nullable List<FibAchievementLeaderboardEntryDto> dtos) {
        List<LeaderboardEntry> rows = new ArrayList<>();
        if (dtos != null) {
            for (FibAchievementLeaderboardEntryDto dto : dtos) {
                rows.add(new LeaderboardEntry(value(dto.getRank()), identity(dto.getPlayer()),
                        value(dto.getCount())));
            }
        }
        return List.copyOf(rows);
    }

    // --- achievements ------------------------------------------------------------------------

    static List<AchievementUnlock> unlocks(@Nullable FibPlayerAchievementsDto dto) {
        List<AchievementUnlock> unlocks = new ArrayList<>();
        if (dto != null && dto.getAchievements() != null) {
            for (FibAchievementDto entry : dto.getAchievements()) {
                unlocks.add(new AchievementUnlock(entry.getAchievementId(),
                        entry.getMode() == null ? null : String.valueOf(entry.getMode()),
                        identity(entry.getTeammate()), entry.getUnlockedAt()));
            }
        }
        return List.copyOf(unlocks);
    }

    // --- collection --------------------------------------------------------------------------

    /** Keyed by item name, which is how the collection screens look an entry up. */
    static Map<String, CollectedItem> collectedItems(@Nullable List<FibFoundItemStatsDto> stats) {
        Map<String, CollectedItem> items = new HashMap<>();
        if (stats != null) {
            for (FibFoundItemStatsDto entry : stats) {
                items.put(entry.getItemName(), new CollectedItem(
                        entry.getFirstCollected() != null ? entry.getFirstCollected().toInstant() : null,
                        value(entry.getTimesCollected())));
            }
        }
        return items;
    }

    static ItemRarity itemRarity(@Nullable FibCollectionRarityDto dto) {
        if (dto == null) {
            return ItemRarity.empty();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        if (dto.getItems() != null) {
            for (FibItemRarityDto item : dto.getItems()) {
                counts.put(item.getItemName(), value(item.getPlayerCount()));
            }
        }
        return new ItemRarity(counts, value(dto.getTotalPlayers()));
    }

    /** The achievement ids a player holds, which is all {@code AchievementStorage} caches. */
    static Set<String> unlockedIds(@Nullable FibPlayerAchievementsDto dto) {
        Set<String> ids = new java.util.HashSet<>();
        if (dto != null && dto.getAchievements() != null) {
            for (FibAchievementDto achievement : dto.getAchievements()) {
                if (achievement.getAchievementId() != null) {
                    ids.add(achievement.getAchievementId());
                }
            }
        }
        return ids;
    }
}
