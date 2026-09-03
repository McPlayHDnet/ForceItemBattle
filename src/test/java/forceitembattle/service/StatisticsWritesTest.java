package forceitembattle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import de.threeseconds.openapi.fibservice.client.model.FibPlayerStatsUpdateRequestDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesUpdateRequestDto;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Rarity;
import forceitembattle.model.Team;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The rules about <em>which row</em> a number belongs on.
 *
 * <p>None of this was reachable before the write seam: the rules shared a class with the generated
 * client, so the only stand-in for them was a mock of the class that owned them. The two mistakes
 * the javadoc in {@link StatisticsWrites} records having already been made are both here — a
 * counting stat sent by both teammates, and a shared peak written to one member row instead of two.
 */
class StatisticsWritesTest {

    /** Lower UUID, so Alice is the primary writer of any team she is in. */
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private RecordingStatisticsSink sink;
    private StatisticsWrites writes;

    @BeforeEach
    void setUp() {
        sink = new RecordingStatisticsSink();
        writes = new StatisticsWrites(sink);
    }

    private static ForceItemPlayer player(UUID uuid) {
        Player bukkit = mock(Player.class);
        Mockito.when(bukkit.getUniqueId()).thenReturn(uuid);
        return new ForceItemPlayer(bukkit, Material.DIRT, 0, 0);
    }

    /** Returns Alice, teamed with Bob. Both entries are wired, as the roster would have them. */
    private static ForceItemPlayer teamed(ForceItemPlayer alice, ForceItemPlayer bob) {
        Team team = new Team(1, Material.STONE, 0, 0, alice, bob);
        alice.setCurrentTeam(team);
        bob.setCurrentTeam(team);
        return alice;
    }

    @Nested
    @DisplayName("a round starting")
    class GameStarted {

        @Test
        void aSoloPlayerCountsTheirOwnGame() {
            writes.recordGameStarted(player(ALICE));

            assertEquals(1, sink.solo.size());
            assertEquals(ALICE, sink.solo.getFirst().player());
            assertEquals(1, sink.solo.getFirst().update().getGamesPlayedAdd());
        }

        /**
         * gamesPlayed counts rather than maxes, and both members address the same normalised row.
         * Sending it from both sides is the doubling this rule exists to prevent.
         */
        @Test
        void onlyThePrimaryWriterCountsTheTeamsGame() {
            ForceItemPlayer alice = player(ALICE);
            ForceItemPlayer bob = player(BOB);
            teamed(alice, bob);

            writes.recordGameStarted(alice);
            writes.recordGameStarted(bob);

            assertEquals(1, sink.team.size());
            assertEquals(1, sink.team.getFirst().update().getGamesPlayedAdd());
        }

        /** A team game never touches the solo table, not even for the member who writes nothing. */
        @Test
        void aTeamGameWritesNoSoloRow() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordGameStarted(alice);

            assertTrue(sink.solo.isEmpty());
        }
    }

    @Nested
    @DisplayName("a round finishing")
    class RoundFinished {

        @Test
        void aSoloWinnerGetsScoreTravelAndTheWinOnTheirOwnRow() {
            writes.recordRoundFinished(player(ALICE), "Alice", 42, 1200L, true);

            assertEquals(1, sink.solo.size());
            var update = sink.solo.getFirst().update();
            assertEquals(42L, update.getHighestScore());
            assertEquals(1200L, update.getBlocksTravelledAdd());
            assertEquals(1, update.getGamesWonAdd());
        }

        @Test
        void aSoloLoserSendsNoWin() {
            writes.recordRoundFinished(player(ALICE), "Alice", 7, 30L, false);

            assertNull(sink.solo.getFirst().update().getGamesWonAdd());
        }

        /**
         * The three scopes, on one call: travel is the player's own and lands on their member row,
         * the score is the team's, and the outcome is player-scoped and always sent.
         */
        @Test
        void aTeamWinnerSplitsAcrossThreeRows() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordRoundFinished(alice, "Alice", 42, 1200L, true);

            assertEquals(1200L, sink.member.getFirst().update().getBlocksTravelledAdd());
            assertEquals(42L, sink.team.getFirst().update().getHighestScore());
            assertEquals(FibPlayerStatsUpdateRequestDto.OutcomeEnum.WIN,
                    sink.outcome.getFirst().update().getOutcome());
            assertTrue(sink.solo.isEmpty());
        }

        /** gamesWon counts, so it follows the same primary-writer rule gamesPlayed does. */
        @Test
        void onlyThePrimaryWriterCountsTheTeamsWin() {
            ForceItemPlayer alice = player(ALICE);
            ForceItemPlayer bob = player(BOB);
            teamed(alice, bob);

            writes.recordRoundFinished(alice, "Alice", 42, 10L, true);
            writes.recordRoundFinished(bob, "Bob", 42, 10L, true);

            assertEquals(2, sink.team.size());
            assertEquals(1, sink.team.getFirst().update().getGamesWonAdd());
            assertNull(sink.team.getLast().update().getGamesWonAdd());
        }

        /**
         * The score maxes rather than counts, so <em>both</em> members send it. Restricting the whole
         * team update to the primary writer would lose the loser's half of a score they share.
         */
        @Test
        void bothMembersSendTheSharedScore() {
            ForceItemPlayer alice = player(ALICE);
            ForceItemPlayer bob = player(BOB);
            teamed(alice, bob);

            writes.recordRoundFinished(alice, "Alice", 42, 10L, false);
            writes.recordRoundFinished(bob, "Bob", 42, 10L, false);

            assertEquals(2, sink.team.size());
            assertEquals(42L, sink.team.getLast().update().getHighestScore());
        }

        /** A loss is what resets a win streak, so losers report an outcome too. */
        @Test
        void aLoserStillReportsAnOutcome() {
            writes.recordRoundFinished(player(ALICE), "Alice", 7, 30L, false);

            assertEquals(FibPlayerStatsUpdateRequestDto.OutcomeEnum.LOSS,
                    sink.outcome.getFirst().update().getOutcome());
            assertEquals("Alice", sink.outcome.getFirst().update().getPlayerName());
        }
    }

    @Nested
    @DisplayName("a find")
    class Find {

        @Test
        void aSoloFindCarriesTheTotalsTheTallyTheTimeAndTheStreak() {
            writes.recordFind(player(ALICE), false, "DIAMOND", false, 3, 5000L);

            var update = sink.solo.getFirst().update();
            assertEquals(1L, update.getTotalItemsFoundAdd());
            assertEquals(Map.of("DIAMOND", 1L), update.getItemCountsAdd());
            assertEquals(3, update.getLongestItemStreak());
            assertEquals(5000L, update.getTotalTimeSpentOnItemsAdd());
        }

        /** A skip breaks the streak, so it is reported on no row at all. */
        @Test
        void aSkipReportsNoStreak() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordFind(alice, true, "DIAMOND", true, 3, 5000L);

            assertTrue(sink.team.isEmpty());
        }

        /** In a team game the streak is the team's; the member row has no field for it. */
        @Test
        void aTeamFindPutsTheStreakOnTheTeamRow() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordFind(alice, true, "DIAMOND", false, 3, 5000L);

            assertEquals(3, sink.team.getFirst().update().getLongestItemStreak());
            assertEquals(1L, sink.member.getFirst().update().getTotalItemsFoundAdd());
        }

        /**
         * An unmeasured find sends no time. Zero is a real value the service would add, and the first
         * find of a round has nothing to measure from.
         */
        @Test
        void anUnmeasuredFindSendsNoTime() {
            writes.recordFind(player(ALICE), false, "DIAMOND", false, 1, 0L);

            assertNull(sink.solo.getFirst().update().getTotalTimeSpentOnItemsAdd());
        }
    }

    @Nested
    @DisplayName("a back-to-back peak")
    class BackToBackPeak {

        @Test
        void aSoloPeakGoesOnTheirOwnRow() {
            writes.recordBackToBackPeak(player(ALICE), false, 4, 0);

            assertEquals(4, sink.solo.getFirst().update().getHighestB2BStreak());
        }

        /** The peak is shared, so both member rows carry it — not only the acting player's. */
        @Test
        void aTeamPeakLandsOnBothMemberRows() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordBackToBackPeak(alice, true, 1, 4);

            assertEquals(2, sink.member.size());
            assertEquals(ALICE, sink.member.getFirst().member());
            assertEquals(BOB, sink.member.getLast().member());
            assertEquals(4, sink.member.getFirst().update().getHighestB2BStreak());
            assertEquals(4, sink.member.getLast().update().getHighestB2BStreak());
        }

        /** Both rows are the same normalised pair; only the member being written to differs. */
        @Test
        void bothWritesAddressTheSamePair() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordBackToBackPeak(alice, true, 1, 4);

            assertEquals(ALICE, sink.member.getFirst().player());
            assertEquals(BOB, sink.member.getFirst().teammate());
            assertEquals(ALICE, sink.member.getLast().player());
            assertEquals(BOB, sink.member.getLast().teammate());
        }
    }

    @Nested
    @DisplayName("a rarity delta")
    class RarityDelta {

        /**
         * A delta carries one rarity. Sending four explicit zeros alongside it is a different
         * payload, and the service adds what it is given.
         */
        @Test
        void onlyTheNonZeroFieldIsSent() {
            writes.recordRarity(ALICE, player(ALICE), Rarity.LEGENDARY);

            FibRaritiesUpdateRequestDto rarities = sink.solo.getFirst().update().getRaritiesAdd();
            assertEquals(1L, rarities.getLegendaryAdd());
            assertNull(rarities.getRareAdd());
            assertNull(rarities.getEpicAdd());
            assertNull(rarities.getRngesusAdd());
            assertNull(rarities.getExtraordinaryAdd());
        }

        @Test
        void aTeamRarityGoesOnTheMemberRow() {
            ForceItemPlayer alice = player(ALICE);
            teamed(alice, player(BOB));

            writes.recordRarity(ALICE, alice, Rarity.RARE);

            assertEquals(1L, sink.member.getFirst().update().getRaritiesAdd().getRareAdd());
            assertTrue(sink.solo.isEmpty());
        }
    }

    @Nested
    @DisplayName("a player counter")
    class Counter {

        @Test
        void aSoloCounterGoesOnTheirOwnRow() {
            writes.recordPlayerCounter(ALICE, player(ALICE), PlayerCounter.DEATHS, 1);

            assertEquals(1L, sink.solo.getFirst().update().getDeathsAdd());
        }

        /** Nobody to attribute it to: a spectator, or someone who connected mid-round. */
        @Test
        void aCounterWithNoRosterEntryWritesNothing() {
            writes.recordPlayerCounter(ALICE, null, PlayerCounter.DEATHS, 1);

            assertEquals(0, sink.writes());
        }
    }
}
