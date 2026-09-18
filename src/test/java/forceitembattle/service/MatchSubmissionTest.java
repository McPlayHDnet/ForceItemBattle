package forceitembattle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.threeseconds.openapi.fibservice.client.model.FibMatchItemSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchParticipantSubmitDto;
import de.threeseconds.openapi.fibservice.client.model.FibMatchSubmitRequestDto;
import forceitembattle.manager.TeamsManager;
import forceitembattle.model.BackToBack;
import forceitembattle.model.ForceItem;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The submission a finished round produces: placement, the win flag, timings, and who is left out. */
class MatchSubmissionTest {

    /** Fixed so the timings below are exact rather than approximately now. */
    private static final long START = 1_000_000L;

    private CapturingMatchSink sink;
    private Roster roster;
    private GameSettings settings;
    private TeamsManager teams;
    private MatchHistoryReporter reporter;

    /** Keeps the one request the reporter builds, and runs the persisted callback the way a PUT would. */
    private static final class CapturingMatchSink implements MatchSink {
        FibMatchSubmitRequestDto request;
        UUID matchId;
        int persistedCalls;

        @Override
        public void submitMatch(UUID matchId, FibMatchSubmitRequestDto request, Runnable onPersisted) {
            this.matchId = matchId;
            this.request = request;
            this.persistedCalls++;
            onPersisted.run();
        }
    }

    @BeforeEach
    void setUp() {
        sink = new CapturingMatchSink();
        roster = new Roster();
        settings = mock(GameSettings.class);
        teams = mock(TeamsManager.class);
        when(teams.getTeams()).thenReturn(new ArrayList<>());

        reporter = new MatchHistoryReporter(sink, roster, settings, teams);
        reporter.beginMatch(UUID.randomUUID());
    }

    private ForceItemPlayer join(String seed, int score) {
        Player bukkit = mock(Player.class);
        when(bukkit.getUniqueId()).thenReturn(
                UUID.fromString("00000000-0000-0000-0000-00000000000" + seed));
        ForceItemPlayer player = new ForceItemPlayer(bukkit, Material.DIRT, 0, score);
        roster.add(player.player().getUniqueId(), player);
        return player;
    }

    /** A find of {@code material} at {@code atMillis}, recorded against whoever owns the score. */
    private static void found(ForceItemPlayer player, Material material, long atMillis, boolean skipped) {
        player.recordFoundItem(new ForceItem(material, "1s", atMillis, new BackToBack(false), skipped,
                player.player().getUniqueId()));
    }

    private FibMatchSubmitRequestDto submit(Map<ForceItemPlayer, Integer> places) {
        reporter.submit(places, null, 900, () -> {
        });
        return sink.request;
    }

    private static FibMatchParticipantSubmitDto participantOf(FibMatchSubmitRequestDto request, UUID uuid) {
        return request.getParticipants().stream()
                .filter(participant -> uuid.equals(participant.getPlayerUuid()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no participant row for " + uuid));
    }

    @Nested
    @DisplayName("participants")
    class Participants {

        @Test
        void theWinnerIsThePlayerPlacedFirst() {
            ForceItemPlayer alice = join("a", 10);
            ForceItemPlayer bob = join("b", 4);

            FibMatchSubmitRequestDto request = submit(Map.of(alice, 1, bob, 2));

            assertTrue(participantOf(request, alice.player().getUniqueId()).getWon());
            assertFalse(participantOf(request, bob.player().getUniqueId()).getWon());
            assertEquals(10L, participantOf(request, alice.player().getUniqueId()).getFinalScore());
        }

        /**
         * A player with no placement is written as 0 rather than left null, because placement is a
         * required column; nobody is placed 0, so it reads as "did not place".
         */
        @Test
        void anUnplacedPlayerIsRecordedAsZeroAndNotAsAWinner() {
            ForceItemPlayer alice = join("a", 0);

            FibMatchSubmitRequestDto request = submit(Map.of());

            assertEquals(0, participantOf(request, alice.player().getUniqueId()).getPlacement());
            assertFalse(participantOf(request, alice.player().getUniqueId()).getWon());
        }

        /** A spectator did not play the match and gets no row on its page. */
        @Test
        void aSpectatorIsLeftOut() {
            ForceItemPlayer alice = join("a", 0);
            ForceItemPlayer watcher = join("b", 0);
            watcher.setSpectator(true);

            FibMatchSubmitRequestDto request = submit(Map.of(alice, 1));

            assertEquals(1, request.getParticipants().size());
            assertEquals(alice.player().getUniqueId(), request.getParticipants().getFirst().getPlayerUuid());
        }

        /** On a team the placement is the team's, and both members carry it. */
        @Test
        void teammatesShareOnePlacement() {
            ForceItemPlayer alice = join("a", 0);
            ForceItemPlayer bob = join("b", 0);
            Team team = new Team(7, Material.STONE, 12, 0, alice, bob);
            alice.setCurrentTeam(team);
            bob.setCurrentTeam(team);

            reporter.submit(Map.of(), Map.of(team, 1), 900, () -> {
            });
            FibMatchSubmitRequestDto request = sink.request;

            for (FibMatchParticipantSubmitDto participant : request.getParticipants()) {
                assertEquals(1, participant.getPlacement());
                assertTrue(participant.getWon());
                assertEquals(7, participant.getTeamIndex());
                assertEquals(12L, participant.getFinalScore());
            }
        }
    }

    @Nested
    @DisplayName("item timings")
    class Timings {

        /** The first item is measured from the match start, every later one from the one before it. */
        @Test
        void eachItemIsMeasuredFromThePreviousHandIn() {
            ForceItemPlayer alice = join("a", 2);
            found(alice, Material.DIRT, START + 30_000L, false);
            found(alice, Material.STONE, START + 50_000L, false);

            List<FibMatchItemSubmitDto> items = submit(Map.of(alice, 1)).getItems();

            assertEquals(2, items.size());
            assertEquals(0, items.getFirst().getOrderIndex());
            assertEquals(20, items.getLast().getSecondsTaken());
        }

        /**
         * The other half of the pause rule, and the half {@link PauseAccountingTest} cannot see: a
         * pause that falls outside an item's window must leave that window alone. The subtraction
         * itself is arithmetic and covered there; that it is applied per-window is covered here.
         */
        @Test
        void aPauseOutsideAWindowLeavesItAlone() {
            ForceItemPlayer alice = join("a", 2);
            found(alice, Material.DIRT, START + 10_000L, false);
            found(alice, Material.STONE, START + 70_000L, false);

            // Recorded at wall-clock now, which is far outside the fixed windows above.
            reporter.onPaused();
            reporter.onResumed();

            assertEquals(60, submit(Map.of(alice, 1)).getItems().getLast().getSecondsTaken());
        }

        /** A skip is still an item on the page, flagged as one. */
        @Test
        void aSkippedItemIsFlagged() {
            ForceItemPlayer alice = join("a", 1);
            found(alice, Material.DIRT, START + 10_000L, true);

            assertTrue(submit(Map.of(alice, 1)).getItems().getFirst().getSkipped());
        }

        /** The rarity is only carried when the back-to-back was actually active. */
        @Test
        void anInactiveBackToBackCarriesNoRarity() {
            ForceItemPlayer alice = join("a", 1);
            found(alice, Material.DIRT, START + 10_000L, false);

            assertNull(submit(Map.of(alice, 1)).getItems().getFirst().getB2bRarity());
        }

        @Test
        void anActiveBackToBackCarriesItsRarity() {
            ForceItemPlayer alice = join("a", 1);
            BackToBack backToBack = new BackToBack(true);
            backToBack.setRarityType(Rarity.LEGENDARY);
            alice.recordFoundItem(new ForceItem(Material.DIRT, "1s", START + 10_000L, backToBack,
                    false, alice.player().getUniqueId()));

            assertEquals("LEGENDARY", submit(Map.of(alice, 1)).getItems().getFirst().getB2bRarity());
        }

        /** Wall-clock timestamps can run backwards over a clock adjustment; a negative is clamped. */
        @Test
        void aBackwardsTimestampIsClampedToZero() {
            ForceItemPlayer alice = join("a", 1);
            found(alice, Material.DIRT, START + 30_000L, false);
            found(alice, Material.STONE, START + 10_000L, false);

            assertEquals(0, submit(Map.of(alice, 1)).getItems().getLast().getSecondsTaken());
        }
    }

    @Nested
    @DisplayName("the submission itself")
    class Submission {

        /** Every setting is snapshotted, so the page can say what the round was played under. */
        @Test
        void everySettingIsSnapshotted() {
            join("a", 0);

            Map<String, String> snapshot = submit(Map.of()).getSettings();

            assertEquals(GameSetting.values().length, snapshot.size());
            for (GameSetting setting : GameSetting.values()) {
                assertNotNull(snapshot.get(setting.name()), setting + " is missing from the snapshot");
            }
        }

        @Test
        void theRequestIsKeyedOnTheMatchId() {
            join("a", 0);
            UUID matchId = UUID.randomUUID();
            reporter.beginMatch(matchId);

            submit(Map.of());

            assertEquals(matchId, sink.matchId);
        }

        /** The game loop hangs collection-achievement evaluation off this; it runs once. */
        @Test
        void thePersistedCallbackRunsOnce() {
            join("a", 0);
            boolean[] ran = {false};

            reporter.submit(Map.of(), null, 900, () -> ran[0] = true);

            assertTrue(ran[0]);
            assertEquals(1, sink.persistedCalls);
        }
    }
}
