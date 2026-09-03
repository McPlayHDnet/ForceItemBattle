package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import java.util.UUID;

/**
 * The four rows a statistics write can land on. See {@code CONTEXT.md § Service Writes}.
 *
 * <p>Deliberately in the generated vocabulary rather than the game's: turning rounds and finds into
 * request DTOs is itself the rule {@link StatisticsWrites} is tested on, so the seam sits below it.
 */
public interface StatisticsSink {

    void updateSolo(UUID playerUuid, FibSoloStatisticsUpdateRequestDto update);

    /** The shared row of a pair. Normalised, so both members address it with the same two UUIDs. */
    void updateTeam(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto update);

    void updateMember(UUID playerUuid, UUID teammateUuid, UUID memberUuid,
                      FibTeamMemberStatsUpdateRequestDto update);

    /** The player-scoped win/loss row, which exists in both modes. */
    void recordOutcome(UUID playerUuid, FibPlayerStatsUpdateRequestDto update);
}
