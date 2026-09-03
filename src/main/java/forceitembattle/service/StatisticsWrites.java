package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.RarityCounts;
import forceitembattle.model.Team;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Which row a statistics number belongs on. See {@code CONTEXT.md § Service Writes}.
 *
 * <p>A stat that <b>counts</b> rather than maxes would be doubled if both teammates sent it, so on a
 * team only the primary writer sends one; a stat that maxes is safe from either side. Leaving that
 * to the call sites is how {@code gamesPlayed} came to be counted twice.
 */
public final class StatisticsWrites {

    private final StatisticsSink sink;

    public StatisticsWrites(StatisticsSink sink) {
        this.sink = sink;
    }

    /** One tick of a counter attributed to the acting player: their solo row, or their member row. */
    public void recordPlayerCounter(UUID self, @Nullable ForceItemPlayer actor,
                                    PlayerCounter counter, long amount) {
        PlayerStatsWrite.record(this.sink, self, actor,
                () -> counter.soloUpdate(amount),
                () -> counter.memberUpdate(amount));
    }

    /** A back-to-back of this rarity, attributed to the acting player. */
    public void recordRarity(UUID self, @Nullable ForceItemPlayer actor, Rarity rarity) {
        PlayerStatsWrite.record(this.sink, self, actor,
                () -> new FibSoloStatisticsUpdateRequestDto().raritiesAdd(raritiesUpdate(rarity)),
                () -> new FibTeamMemberStatsUpdateRequestDto().raritiesAdd(raritiesUpdate(rarity)));
    }

    /** A skip breaks the streak, so it is reported on no row; in a team it belongs to the team row. */
    public void recordFind(ForceItemPlayer finder, boolean teamGame, String itemName,
                           boolean skipped, int itemStreak, long timeSpentMs) {
        UUID self = finder.player().getUniqueId();

        if (teamGame && !skipped) {
            finder.teammate().ifPresent(teammate -> this.sink.updateTeam(
                    self, teammate.player().getUniqueId(),
                    new FibTeamStatisticsUpdateRequestDto().longestItemStreak(itemStreak)));
        }

        PlayerStatsWrite.record(this.sink, self, finder,
                () -> {
                    FibSoloStatisticsUpdateRequestDto update = new FibSoloStatisticsUpdateRequestDto()
                            .totalItemsFoundAdd(1L)
                            .itemCountsAdd(Map.of(itemName, 1L));
                    if (!skipped) {
                        update.longestItemStreak(itemStreak);
                    }
                    if (timeSpentMs > 0) {
                        update.totalTimeSpentOnItemsAdd(timeSpentMs);
                    }
                    return update;
                },
                () -> {
                    FibTeamMemberStatsUpdateRequestDto update = new FibTeamMemberStatsUpdateRequestDto()
                            .totalItemsFoundAdd(1L)
                            .itemCountsAdd(Map.of(itemName, 1L));
                    if (timeSpentMs > 0) {
                        update.totalTimeSpentOnItemsAdd(timeSpentMs);
                    }
                    return update;
                });
    }

    /** A team's peak is shared, so it goes on <em>both</em> member rows, not just the acting one. */
    public void recordBackToBackPeak(ForceItemPlayer player, boolean teamGame,
                                     int ownStreak, int teamStreak) {
        UUID self = player.player().getUniqueId();

        if (!teamGame) {
            this.sink.updateSolo(self,
                    new FibSoloStatisticsUpdateRequestDto().highestB2BStreak(ownStreak));
            return;
        }

        player.teammate().ifPresent(teammate -> {
            UUID other = teammate.player().getUniqueId();
            this.sink.updateMember(self, other, self,
                    new FibTeamMemberStatsUpdateRequestDto().highestB2BStreak(teamStreak));
            this.sink.updateMember(self, other, other,
                    new FibTeamMemberStatsUpdateRequestDto().highestB2BStreak(teamStreak));
        });
    }


    public void recordGameStarted(ForceItemPlayer player) {
        UUID self = player.player().getUniqueId();
        Team team = player.currentTeam();

        if (team == null) {
            this.sink.updateSolo(self, new FibSoloStatisticsUpdateRequestDto().gamesPlayedAdd(1));
            return;
        }

        if (team.isPrimaryWriter(player)) {
            player.teammate().ifPresent(teammate -> this.sink.updateTeam(
                    self, teammate.player().getUniqueId(),
                    new FibTeamStatisticsUpdateRequestDto().gamesPlayedAdd(1)));
        }
    }

    /**
     * One method rather than three, because the three numbers have three scopes: travel is the
     * player's own, the score and the win belong to whoever owns the score, and the win/loss outcome
     * is player-scoped and reported by <em>everyone</em> — a loss is what resets a streak.
     */
    public void recordRoundFinished(ForceItemPlayer player, String playerName,
                                    int score, long blocksTravelled, boolean won) {
        UUID self = player.player().getUniqueId();
        Team team = player.currentTeam();

        PlayerStatsWrite.record(this.sink, self, player,
                () -> {
                    FibSoloStatisticsUpdateRequestDto update = new FibSoloStatisticsUpdateRequestDto()
                            .blocksTravelledAdd(blocksTravelled)
                            .highestScore((long) score);
                    if (won) {
                        update.gamesWonAdd(1);
                    }
                    return update;
                },
                () -> new FibTeamMemberStatsUpdateRequestDto().blocksTravelledAdd(blocksTravelled));

        if (team != null) {
            player.teammate().ifPresent(teammate -> {
                FibTeamStatisticsUpdateRequestDto update = new FibTeamStatisticsUpdateRequestDto()
                        .highestScore((long) score);
                if (won && team.isPrimaryWriter(player)) {
                    update.gamesWonAdd(1);
                }
                this.sink.updateTeam(self, teammate.player().getUniqueId(), update);
            });
        }

        this.sink.recordOutcome(self, new FibPlayerStatsUpdateRequestDto()
                .outcome(won
                        ? FibPlayerStatsUpdateRequestDto.OutcomeEnum.WIN
                        : FibPlayerStatsUpdateRequestDto.OutcomeEnum.LOSS)
                .playerName(playerName));
    }

    /** Only non-zero fields: a delta carries one, and four explicit zeros is a different payload. */
    private static FibRaritiesUpdateRequestDto raritiesUpdate(Rarity rarity) {
        RarityCounts counts = rarity.asIncrement();
        FibRaritiesUpdateRequestDto request = new FibRaritiesUpdateRequestDto();

        if (counts.rare() != 0) request.rareAdd(counts.rare());
        if (counts.epic() != 0) request.epicAdd(counts.epic());
        if (counts.legendary() != 0) request.legendaryAdd(counts.legendary());
        if (counts.rngesus() != 0) request.rngesusAdd(counts.rngesus());
        if (counts.extraordinary() != 0) request.extraordinaryAdd(counts.extraordinary());

        return request;
    }
}
