package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import java.util.UUID;

/** In the generated vocabulary on purpose: building the DTOs is the rule {@link StatisticsWrites} is tested on. */
public interface StatisticsSink {

    void updateSolo(UUID playerUuid, FibSoloStatisticsUpdateRequestDto update);

    /** The shared row of a pair. Normalised, so both members address it with the same two UUIDs. */
    void updateTeam(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto update);

    void updateMember(UUID playerUuid, UUID teammateUuid, UUID memberUuid,
                      FibTeamMemberStatsUpdateRequestDto update);

    /** The player-scoped win/loss row, which exists in both modes. */
    void recordOutcome(UUID playerUuid, FibPlayerStatsUpdateRequestDto update);
}
