package forceitembattle.service;

import forceitembattle.model.Roster;
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
 * <p>Two suppliers rather than one value because the generated client has separate builders for solo
 * and member updates with no common supertype. Only the branch that runs is built.
 *
 * <p>Deliberately not for shared team stats (highest score, longest streak): those live on the team
 * row, are written once per team, and go through {@code updateTeamStatisticsAsync} guarded by
 * {@link forceitembattle.model.Team#isPrimaryWriter(ForceItemPlayer)}.
 *
 * <p><b>Who counts as a participant is decided here, not at the call sites</b>, so the next caller
 * cannot forget it. Leaving it to each site put a spectator's teleporter use on their solo row,
 * surfacing on the stats page as a player whose entire career is one teleporter use.
 */
public final class PlayerStatsWrite {

    private PlayerStatsWrite() {
    }

    /**
     * @param self           the acting player's UUID — passed separately because it addresses the
     *                       row, and the roster entry is consulted only to decide which row
     * @param forceItemPlayer the acting player's roster entry, or {@code null} if they have none
     */
    public static void record(FibStatisticsClient statistics,
                              UUID self,
                              @Nullable ForceItemPlayer forceItemPlayer,
                              Supplier<FibSoloStatisticsUpdateRequestDto> soloUpdate,
                              Supplier<FibTeamMemberStatsUpdateRequestDto> memberUpdate) {
        // Both shapes of "watching rather than playing": the spectate toggle keeps a roster entry
        // with the flag set, while someone who connected after the round began has none at all.
        if (!Roster.isPlaying(forceItemPlayer)) {
            return;
        }

        if (!forceItemPlayer.isInTeam()) {
            statistics.updateSoloStatisticsAsync(self, soloUpdate.get());
            return;
        }

        // An odd player count leaves someone in a one-person team. There is no member row to write
        // to, so they are skipped rather than recorded against themselves.
        forceItemPlayer.teammate().ifPresent(teammate -> {
            if (teammate.player() == null) {
                return;
            }
            statistics.updateMemberStatisticsAsync(
                    self, teammate.player().getUniqueId(), self, memberUpdate.get());
        });
    }
}
