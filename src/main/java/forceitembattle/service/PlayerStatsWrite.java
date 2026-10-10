package forceitembattle.service;

import forceitembattle.model.Roster;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import forceitembattle.model.ForceItemPlayer;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * One player's own contribution, to their solo row or their team member row. Shared team stats go
 * through {@code updateTeam} instead. Participation is checked here so no caller can forget it.
 */
public final class PlayerStatsWrite {

    private PlayerStatsWrite() {
    }

    /**
     * @param self            addresses the row; the roster entry only decides which one
     * @param forceItemPlayer the acting player's roster entry, or {@code null} if they have none
     */
    public static void record(StatisticsSink sink,
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
            sink.updateSolo(self, soloUpdate.get());
            return;
        }

        // An odd player count leaves someone in a one-person team. There is no member row to write
        // to, so they are skipped rather than recorded against themselves.
        forceItemPlayer.teammate().ifPresent(teammate -> {
            if (teammate.player() == null) {
                return;
            }
            sink.updateMember(
                    self, teammate.player().getUniqueId(), self, memberUpdate.get());
        });
    }
}
