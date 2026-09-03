package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import java.util.UUID;

/**
 * The four rows a statistics write can land on, with the transport taken out.
 *
 * <p>This exists so {@link StatisticsWrites} — which owns the rules about <em>which</em> row a
 * number belongs on — can be exercised without HTTP. Those rules are the part that has been got
 * wrong before (gamesPlayed counted twice, a peak written to one member row instead of both), and
 * while they lived in the same class as the generated client the only stand-in for them was a mock
 * of the class that owned them.
 *
 * <p>It stays in the generated vocabulary rather than the game's on purpose. Translating rounds and
 * finds into request DTOs <em>is</em> the rule under test, so the seam has to sit below it; a sink
 * taking domain types would move the interesting half back behind the interface. The DTOs are plain
 * builders and need no server, so this is still headless.
 *
 * <p>The production implementation is {@link FibStatisticsClient} itself, which invalidates the
 * global-stats cache and hands the call to {@link ApiExecutor}.
 */
public interface StatisticsSink {

    /** A player's own row in a solo game. */
    void updateSolo(UUID playerUuid, FibSoloStatisticsUpdateRequestDto update);

    /** The shared row of a pair. Normalised, so both members address it with the same two UUIDs. */
    void updateTeam(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto update);

    /** One member's row inside a pair — {@code memberUuid} says whose. */
    void updateMember(UUID playerUuid, UUID teammateUuid, UUID memberUuid,
                      FibTeamMemberStatsUpdateRequestDto update);

    /** The player-scoped win/loss row, which exists in both modes. */
    void recordOutcome(UUID playerUuid, FibPlayerStatsUpdateRequestDto update);
}
