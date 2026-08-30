package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import java.util.function.BiFunction;

/**
 * A running total the game keeps about one player's own doing, named in the game's words rather
 * than the generated client's.
 *
 * <h2>Why an enum rather than two lambdas at the call site</h2>
 *
 * Each of these is stored twice — on a player's solo row, and on their member row inside a team —
 * under a field the OpenAPI generator named. There is no common supertype for the two builders, so
 * every caller used to hand in a pair of suppliers:
 *
 * <pre>{@code
 * PlayerStatsWrite.record(statistics, uuid, player,
 *         () -> FIBServiceClient.soloUpdate().deathsAdd(1L),
 *         () -> FIBServiceClient.memberUpdate().deathsAdd(1L));
 * }</pre>
 *
 * <p>Which meant a listener had to know both generated types, both builder methods, and that the
 * two must stay in step. Pairing them once here leaves the caller naming a counter and an amount,
 * and puts the only place the two halves can drift out of step behind the seam.
 */
public enum PlayerCounter {

    DEATHS(
            FibSoloStatisticsUpdateRequestDto::deathsAdd,
            FibTeamMemberStatsUpdateRequestDto::deathsAdd),

    WHEELS_OF_FORTUNE_USED(
            FibSoloStatisticsUpdateRequestDto::wheelOfFortuneUsesAdd,
            FibTeamMemberStatsUpdateRequestDto::wheelOfFortuneUsesAdd),

    ANTIMATTER_TELEPORTER_ENTRIES(
            FibSoloStatisticsUpdateRequestDto::enteredAntimatterTeleporterAdd,
            FibTeamMemberStatsUpdateRequestDto::enteredAntimatterTeleporterAdd);

    private final BiFunction<FibSoloStatisticsUpdateRequestDto, Long, FibSoloStatisticsUpdateRequestDto> onSolo;
    private final BiFunction<FibTeamMemberStatsUpdateRequestDto, Long, FibTeamMemberStatsUpdateRequestDto> onMember;

    PlayerCounter(
            BiFunction<FibSoloStatisticsUpdateRequestDto, Long, FibSoloStatisticsUpdateRequestDto> onSolo,
            BiFunction<FibTeamMemberStatsUpdateRequestDto, Long, FibTeamMemberStatsUpdateRequestDto> onMember) {
        this.onSolo = onSolo;
        this.onMember = onMember;
    }

    FibSoloStatisticsUpdateRequestDto soloUpdate(long amount) {
        return this.onSolo.apply(new FibSoloStatisticsUpdateRequestDto(), amount);
    }

    FibTeamMemberStatsUpdateRequestDto memberUpdate(long amount) {
        return this.onMember.apply(new FibTeamMemberStatsUpdateRequestDto(), amount);
    }
}
