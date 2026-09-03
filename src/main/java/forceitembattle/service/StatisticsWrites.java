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
 * The game's vocabulary for a statistics write: rounds, finds and players in, request rows out.
 *
 * <p><b>The rules about which row a number belongs on live here, not at the call sites.</b> Leaving
 * them at the call sites is how {@code gamesPlayed} came to be counted twice once already. A stat
 * that counts rather than maxes would be doubled if both teammates sent it, so on a team only the
 * primary writer sends one; a stat that maxes is safe from either side.
 *
 * <p>It holds a {@link StatisticsSink} and nothing else — no HTTP, no plugin, no Bukkit beyond the
 * roster types it is handed. That is the whole point of the split: every rule below is reachable
 * from a test with a recording sink, and each one had no coverage at all while it shared a class
 * with the generated client.
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

    /**
     * One item obtained: the totals, the per-item tally, the time it took, and the streak. In a team
     * game the streak peak belongs on the shared team row; solo, it rides along on the player's own
     * update. A skip breaks the streak, so it is reported on neither.
     */
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

    /**
     * The peak of a back-to-back chain. Solo keeps their own; a team's peak is shared, so it is
     * written to <em>both</em> member rows rather than only the acting player's.
     */
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

    /**
     * Both teammates write the same normalised team row, so a stat that counts rather than maxes
     * would be doubled if both sides sent it — only the primary writer does.
     */
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
     * A player finished a round. One method rather than three calls because the three numbers have
     * three different scopes:
     * <ul>
     *   <li>travel is the player's own contribution, so it routes solo-or-member;</li>
     *   <li>score and the win go on whichever row owns the score — and {@code gamesWon} counts, so
     *       on a team only the primary writer sends it;</li>
     *   <li>the win/loss outcome is player-scoped and <em>everyone</em> reports it, winners and
     *       losers alike, because a loss is what resets a streak.</li>
     * </ul>
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

    /**
     * A rarity delta as the generated request wants it. Only non-zero fields are set: a delta carries
     * a single one, and sending four explicit zeros alongside it is a different payload.
     */
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
