package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import forceitembattle.model.ForceItemPlayer;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Routes a stat that belongs to <em>one player's own contribution</em> to whichever row records it:
 * their solo stats in a solo game, their member row inside the team in a team game.
 *
 * <p>Five call sites (found item, back-to-back rarity, wheel-of-fortune use, death, antimatter
 * teleporter) all wrote the same ten-line branch, differing only in the payload. They differ in
 * <em>type</em> as well — the generated client has separate builders for solo and member updates
 * with no common supertype — which is why this takes two suppliers rather than one value. Only the
 * branch that runs is built.
 *
 * <p>Deliberately not for shared team stats (highest score, longest streak): those live on the team
 * row, are written once per team, and go through {@code updateTeamStatisticsAsync} guarded by
 * {@link forceitembattle.model.Team#isPrimaryWriter(ForceItemPlayer)}.
 */
public final class PlayerStatsWrite {

    private PlayerStatsWrite() {
    }

    /**
     * @param self           the acting player's UUID — passed separately because the solo branch
     *                       has to work even for someone with no roster entry, which is what the
     *                       hand-rolled {@code fip != null && fip.currentTeam() != null} checks did
     * @param forceItemPlayer the acting player's roster entry, or {@code null} if they have none
     */
    public static void record(FibStatisticsClient statistics,
                              UUID self,
                              @Nullable ForceItemPlayer forceItemPlayer,
                              Supplier<FibSoloStatisticsUpdateRequestDto> soloUpdate,
                              Supplier<FibTeamMemberStatsUpdateRequestDto> memberUpdate) {
        if (forceItemPlayer == null || !forceItemPlayer.isInTeam()) {
            statistics.updateSoloStatisticsAsync(self, soloUpdate.get());
            return;
        }

        // An odd player count leaves someone in a one-person team. They have no member row to write
        // to, and the old hand-rolled loops silently skipped them too — keep that.
        forceItemPlayer.teammate().ifPresent(teammate -> {
            if (teammate.player() == null) {
                return;
            }
            statistics.updateMemberStatisticsAsync(
                    self, teammate.player().getUniqueId(), self, memberUpdate.get());
        });
    }
}
