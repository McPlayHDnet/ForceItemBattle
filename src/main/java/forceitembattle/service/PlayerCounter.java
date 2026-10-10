package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import java.util.function.BiFunction;

/** Each counter is stored on the solo row and the member row, whose builders share no supertype. */
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
