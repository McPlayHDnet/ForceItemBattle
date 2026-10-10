package forceitembattle.service;

import de.threeseconds.openapi.fibservice.client.model.FibMatchItemSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchParticipantSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchTeamSubmitDto;
import forceitembattle.manager.TeamsManager;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;

/** Telemetry, not game loop. All state is per-match and reset by {@link #beginMatch(UUID)}. */
public class MatchHistoryReporter {

    private static final String MATCH_STATS_URL = "https://forceitembattle.net/stats?view=match&id=";
    private static final long MATCH_STATS_LINK_DELAY_TICKS = 5L;


    @Getter
    private UUID matchId;
    private long startedAtMillis;

    private final LeadTracker leadTracker = new LeadTracker();

    /** True once the match-history PUT landed; the stats link is held back until the result reveal. */
    private boolean linkReady;
    /** True once every result has been revealed via /result (the winner comes last). */
    private boolean resultsRevealed;
    /** Guard so the stats link is broadcast exactly once per match. */
    private boolean linkShared;

    /** Closed [start, end] millis pairs, subtracted at submit so seconds_taken is play time, not wall time. */
    private final List<long[]> pauseIntervals = new ArrayList<>();

    /** Wall-clock millis when the current pause began, or 0 when the game is not paused. */
    private long pauseStartedAt;

    private final MatchSink matchSink;
    private final Roster roster;
    private final GameSettings settings;
    private final TeamsManager teamManager;

    public MatchHistoryReporter(MatchSink matchSink, Roster roster, GameSettings settings,
                                TeamsManager teamManager) {
        this.matchSink = matchSink;
        this.roster = roster;
        this.settings = settings;
        this.teamManager = teamManager;
    }

    public void beginMatch(UUID matchId) {
        this.matchId = matchId;
        this.startedAtMillis = System.currentTimeMillis();
        this.leadTracker.reset();
        this.pauseIntervals.clear();
        this.pauseStartedAt = 0L;
        this.linkReady = false;
        this.resultsRevealed = false;
        this.linkShared = false;
    }

    /** A change of sole leader counts as a lead change. */
    public void recordStandings(Object soleLeader) {
        this.leadTracker.onStandingsChanged(soleLeader);
    }

    public void onPaused() {
        this.pauseStartedAt = System.currentTimeMillis();
    }

    public void onResumed() {
        if (this.pauseStartedAt > 0L) {
            this.pauseIntervals.add(new long[]{this.pauseStartedAt, System.currentTimeMillis()});
            this.pauseStartedAt = 0L;
        }
    }

    /** The stats link goes out only once both this and the match PUT have completed. */
    public void markResultsRevealed() {
        this.resultsRevealed = true;
        this.tryShareLink();
    }

    /** @param onPersisted runs once the PUT lands, so collection achievements see this match */
    public void submit(Map<ForceItemPlayer, Integer> placesMap,
                       Map<Team, Integer> teamPlaces,
                       int durationSeconds,
                       Runnable onPersisted) {
        boolean teamMode = this.settings.isSettingEnabled(GameSetting.TEAM);
        Map<UUID, ForceItemPlayer> roster = this.roster.players();

        FibMatchSubmitRequestDto request = new FibMatchSubmitRequestDto()
                .startedAt(Instant.ofEpochMilli(this.startedAtMillis).atOffset(ZoneOffset.UTC))
                .endedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .durationSeconds(durationSeconds)
                .mode(teamMode ? FibMatchSubmitRequestDto.ModeEnum.TEAM : FibMatchSubmitRequestDto.ModeEnum.SOLO)
                .leadChanges(this.leadTracker.leadChanges())
                .teams(teamMode ? buildTeams() : List.of())
                .participants(buildParticipants(roster, placesMap, teamPlaces))
                .items(buildItems(roster, teamMode))
                .settings(snapshotSettings(this.settings));

        this.matchSink.submitMatch(this.matchId, request, () -> {
            onPersisted.run();
            this.linkReady = true;
            this.tryShareLink();
        });
    }

    /** GameSetting constant name -> value as text. Integer-valued settings are not toggles. */
    private Map<String, String> snapshotSettings(GameSettings settings) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (GameSetting setting : GameSetting.values()) {
            String value = setting.defaultValue() instanceof Integer
                    ? String.valueOf(settings.getSettingValue(setting))
                    : String.valueOf(settings.isSettingEnabled(setting));
            snapshot.put(setting.name(), value);
        }
        return snapshot;
    }

    private List<FibMatchTeamSubmitDto> buildTeams() {
        List<FibMatchTeamSubmitDto> teams = new ArrayList<>();
        for (Team team : this.teamManager.getTeams()) {
            teams.add(new FibMatchTeamSubmitDto()
                    .teamIndex(team.getTeamId())
                    .teamName(team.getName())
                    .color(team.getColor() != null ? team.getColor().name() : null));
        }
        return teams;
    }

    private List<FibMatchParticipantSubmitDto> buildParticipants(Map<UUID, ForceItemPlayer> roster,
                                                                 Map<ForceItemPlayer, Integer> placesMap,
                                                                 Map<Team, Integer> teamPlaces) {
        List<FibMatchParticipantSubmitDto> participants = new ArrayList<>();
        for (ForceItemPlayer forceItemPlayer : roster.values()) {
            if (forceItemPlayer.isSpectator()) {
                continue;
            }
            Team team = forceItemPlayer.currentTeam();
            Integer placement = team == null
                    ? placesMap.get(forceItemPlayer)
                    : (teamPlaces != null ? teamPlaces.get(team) : null);
            participants.add(new FibMatchParticipantSubmitDto()
                    .playerUuid(forceItemPlayer.player().getUniqueId())
                    .teamIndex(team == null ? null : team.getTeamId())
                    .finalScore((long) forceItemPlayer.activeScore())
                    .placement(placement != null ? placement : 0)
                    .won(placement != null && placement == 1));
        }
        return participants;
    }

    private List<FibMatchItemSubmitDto> buildItems(Map<UUID, ForceItemPlayer> roster, boolean teamMode) {
        List<FibMatchItemSubmitDto> items = new ArrayList<>();
        if (teamMode) {
            for (Team team : this.teamManager.getTeams()) {
                appendItems(items, team.foundItems(), null, team.getTeamId());
            }
        } else {
            for (ForceItemPlayer forceItemPlayer : roster.values()) {
                if (forceItemPlayer.isSpectator()) {
                    continue;
                }
                appendItems(items, forceItemPlayer.foundItems(),
                        forceItemPlayer.player().getUniqueId(), null);
            }
        }
        return items;
    }

    private void appendItems(List<FibMatchItemSubmitDto> out, List<ForceItem> found,
                             UUID playerUuid, Integer teamIndex) {
        List<Long> secondsTaken = this.secondsTaken(found);
        for (int i = 0; i < found.size(); i++) {
            ForceItem forceItem = found.get(i);
            String b2bRarity = (forceItem.back2Back() != null && forceItem.back2Back().isActive()
                    && forceItem.back2Back().getRarityType() != null)
                    ? forceItem.back2Back().getRarityType().name()
                    : null;
            out.add(new FibMatchItemSubmitDto()
                    .playerUuid(playerUuid)
                    .teamIndex(teamIndex)
                    .itemName(forceItem.material().getKey().asString())
                    .skipped(forceItem.usedSkip())
                    .b2bRarity(b2bRarity)
                    .orderIndex(i)
                    .secondsTaken(secondsTaken.get(i).intValue())
                    .collectedAt(Instant.ofEpochMilli(forceItem.timeStamp()).atOffset(ZoneOffset.UTC)));
        }
    }

    /** From timestamps, since {@code ForceItem.timeNeeded} is a display string, not a duration. */
    public List<Long> secondsTaken(List<ForceItem> found) {
        List<Long> seconds = new ArrayList<>(found.size());
        long previousMillis = this.startedAtMillis;
        for (ForceItem forceItem : found) {
            // Minus any pause inside the span, or an item straddling a long pause tops the "biggest timesinks".
            long rawMillis = forceItem.timeStamp() - previousMillis;
            long playMillis = rawMillis - pausedMillisWithin(previousMillis, forceItem.timeStamp());
            seconds.add(Math.max(0L, playMillis / 1000L));
            previousMillis = forceItem.timeStamp();
        }
        return seconds;
    }

    private long pausedMillisWithin(long fromMillis, long toMillis) {
        return pausedMillisWithin(this.pauseIntervals, fromMillis, toMillis);
    }

    static long pausedMillisWithin(List<long[]> intervals, long fromMillis, long toMillis) {
        long paused = 0L;
        for (long[] interval : intervals) {
            long overlapStart = Math.max(fromMillis, interval[0]);
            long overlapEnd = Math.min(toMillis, interval[1]);
            if (overlapEnd > overlapStart) {
                paused += overlapEnd - overlapStart;
            }
        }
        return paused;
    }

    /** Only once the match is persisted and the /result reveal is over; earlier would spoil the winner. */
    private void tryShareLink() {
        if (!this.linkReady || !this.resultsRevealed || this.linkShared) {
            return;
        }
        this.linkShared = true;

        String matchUrl = MATCH_STATS_URL + this.matchId;
        Scheduler.runLaterSync(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>The match has concluded — see the full breakdown:"));
            player.sendMessage(Text.of("<dark_gray>» <click:open_url:'" + Text.tagArgument(matchUrl)
                    + "'><hover:show_text:'<gray>Opens the match stats in your browser'>"
                    + "<dark_gray>[<aqua><b>View Match Stats</b><dark_gray>]</hover></click>"));
            player.sendMessage(" ");
        }), MATCH_STATS_LINK_DELAY_TICKS);
    }
}
