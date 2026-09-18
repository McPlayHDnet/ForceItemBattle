package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsUpdateRequestDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Keeps every write in order with the row it was addressed to, so a test can assert both which row
 * and what was on it — the second being awkward with a Mockito mock, since the payload is a builder.
 */
final class RecordingStatisticsSink implements StatisticsSink {

    record Solo(UUID player, FibSoloStatisticsUpdateRequestDto update) {
    }

    record Team(UUID player, UUID teammate, FibTeamStatisticsUpdateRequestDto update) {
    }

    record Member(UUID player, UUID teammate, UUID member, FibTeamMemberStatsUpdateRequestDto update) {
    }

    record Outcome(UUID player, FibPlayerStatsUpdateRequestDto update) {
    }

    final List<Solo> solo = new ArrayList<>();
    final List<Team> team = new ArrayList<>();
    final List<Member> member = new ArrayList<>();
    final List<Outcome> outcome = new ArrayList<>();

    @Override
    public void updateSolo(UUID playerUuid, FibSoloStatisticsUpdateRequestDto update) {
        this.solo.add(new Solo(playerUuid, update));
    }

    @Override
    public void updateTeam(UUID playerUuid, UUID teammateUuid, FibTeamStatisticsUpdateRequestDto update) {
        this.team.add(new Team(playerUuid, teammateUuid, update));
    }

    @Override
    public void updateMember(UUID playerUuid, UUID teammateUuid, UUID memberUuid,
                             FibTeamMemberStatsUpdateRequestDto update) {
        this.member.add(new Member(playerUuid, teammateUuid, memberUuid, update));
    }

    @Override
    public void recordOutcome(UUID playerUuid, FibPlayerStatsUpdateRequestDto update) {
        this.outcome.add(new Outcome(playerUuid, update));
    }

    /** Every write this sink saw, for the assertions that only care that nothing happened. */
    int writes() {
        return this.solo.size() + this.team.size() + this.member.size() + this.outcome.size();
    }
}
