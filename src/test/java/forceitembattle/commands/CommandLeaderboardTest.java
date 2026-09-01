package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandLeaderboard;
import forceitembattle.model.stats.DuoLeaderboardEntry;
import forceitembattle.model.stats.LeaderboardEntry;
import forceitembattle.model.stats.PlayerIdentity;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibAchievementClient;
import forceitembattle.service.FibStatisticsClient;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /top}: which board is asked for, and what an unrecognised word does.
 *
 * <p>Four scopes over three service calls, dispatched by two whitelists. The parts that repay a
 * test:
 *
 * <ul>
 *   <li><b>The defaults.</b> A bare {@code /top} means solo, ranked by highest score, and it gets
 *       there by defaulting {@code args[0]} <em>before</em> the whitelist check rather than by
 *       branching on {@code args.length}. That ordering is load-bearing: the refusal it would
 *       otherwise fall into reads {@code args[0]}, so a defaulting mistake here is an
 *       {@code ArrayIndexOutOfBoundsException} on the most common invocation of the command.</li>
 *   <li><b>Scope and category are separate whitelists</b> that produce different refusals, and a
 *       valid word from the wrong list is not accepted by the other.</li>
 *   <li><b>{@code achievements} takes no category</b> and returns before the category whitelist is
 *       consulted, so {@code /top achievements} works where the same shape on any other scope
 *       would be checked. It also reads a different service entirely.</li>
 * </ul>
 *
 * <p>The rows themselves are asserted once per scope, because the duo board is the only one that
 * renders a pair and the solo and teams boards differ only in their heading.
 */
class CommandLeaderboardTest {

    private ServerMock server;
    private FibStatisticsClient statistics;
    private FibAchievementClient achievements;
    private CommandLeaderboard command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        FIBServiceClient service = mock(FIBServiceClient.class);
        this.statistics = mock(FibStatisticsClient.class);
        this.achievements = mock(FibAchievementClient.class);

        when(plugin.getFibService()).thenReturn(service);
        when(service.statistics()).thenReturn(this.statistics);
        when(service.achievements()).thenReturn(this.achievements);

        this.command = new CommandLeaderboard(plugin);
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private PlayerMock join(String name) {
        return this.server.addPlayer(name);
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "top", args);
    }

    private static LeaderboardEntry row(int rank, String name, long value) {
        return new LeaderboardEntry(rank, new PlayerIdentity(UUID.randomUUID(), name), value);
    }

    /** Arms {@code soloLeaderboard} to hand these rows back. */
    private void soloBoardOf(List<LeaderboardEntry> entries) {
        doAnswer(invocation -> {
            invocation.<Consumer<List<LeaderboardEntry>>>getArgument(2).accept(entries);
            return null;
        }).when(this.statistics).soloLeaderboard(any(), anyInt(), any(), any());
    }

    private void teamsBoardOf(List<LeaderboardEntry> entries) {
        doAnswer(invocation -> {
            invocation.<Consumer<List<LeaderboardEntry>>>getArgument(2).accept(entries);
            return null;
        }).when(this.statistics).combinedTeamLeaderboard(any(), anyInt(), any(), any());
    }

    private void duoBoardOf(List<DuoLeaderboardEntry> entries) {
        doAnswer(invocation -> {
            invocation.<Consumer<List<DuoLeaderboardEntry>>>getArgument(2).accept(entries);
            return null;
        }).when(this.statistics).duoLeaderboard(any(), anyInt(), any(), any());
    }

    private void achievementBoardOf(List<LeaderboardEntry> entries) {
        doAnswer(invocation -> {
            invocation.<Consumer<List<LeaderboardEntry>>>getArgument(1).accept(entries);
            return null;
        }).when(this.achievements).achievementLeaderboard(anyInt(), any(), any());
    }

    private void soloBoardFails() {
        doAnswer(invocation -> {
            invocation.<Consumer<Object>>getArgument(3).accept(null);
            return null;
        }).when(this.statistics).soloLeaderboard(any(), anyInt(), any(), any());
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class Defaults {

        /**
         * A bare {@code /top} is the common case and the one the argument handling is riskiest
         * on: the scope is defaulted before the whitelist runs, and the whitelist's refusal
         * reads {@code args[0]}.
         */
        @Test
        void aBareTopIsTheSoloHighestScoreBoard() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of(row(1, "Understudy1", 90)));

            run(player);

            verify(statistics).soloLeaderboard(eq("highest_score"), eq(10), any(), any());
        }

        @Test
        void aScopeWithNoCategoryStillDefaultsTheCategory() {
            PlayerMock player = join("Understudy1");
            duoBoardOf(List.of());

            run(player, "duo");

            verify(statistics).duoLeaderboard(eq("highest_score"), eq(10), any(), any());
        }

        @Test
        void bothWordsAreCaseInsensitive() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of());

            run(player, "SOLO", "GAMES_WON");

            verify(statistics).soloLeaderboard(eq("games_won"), eq(10), any(), any());
        }

        /** Ten rows, not the service's own default: the command decides the depth. */
        @Test
        void everyBoardAsksForTheSameTopTen() {
            PlayerMock player = join("Understudy1");
            teamsBoardOf(List.of());
            achievementBoardOf(List.of());

            run(player, "teams");
            run(player, "achievements");

            verify(statistics).combinedTeamLeaderboard(any(), eq(10), any(), any());
            verify(achievements).achievementLeaderboard(eq(10), any(), any());
        }
    }

    @Nested
    class Scopes {

        @Test
        void soloReadsTheSoloBoard() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of());

            run(player, "solo");

            verify(statistics).soloLeaderboard(any(), anyInt(), any(), any());
        }

        /** "teams" is the across-all-partners board, not the per-pair one. */
        @Test
        void teamsReadsTheCombinedBoardAndNotTheDuoOne() {
            PlayerMock player = join("Understudy1");
            teamsBoardOf(List.of());

            run(player, "teams");

            verify(statistics).combinedTeamLeaderboard(any(), anyInt(), any(), any());
            verify(statistics, never()).duoLeaderboard(any(), anyInt(), any(), any());
        }

        @Test
        void duoReadsThePerPairBoardAndNotTheCombinedOne() {
            PlayerMock player = join("Understudy1");
            duoBoardOf(List.of());

            run(player, "duo");

            verify(statistics).duoLeaderboard(any(), anyInt(), any(), any());
            verify(statistics, never()).combinedTeamLeaderboard(any(), anyInt(), any(), any());
        }

        /** Achievements is the one scope served by a different client entirely. */
        @Test
        void achievementsReadsTheAchievementServiceInstead() {
            PlayerMock player = join("Understudy1");
            achievementBoardOf(List.of());

            run(player, "achievements");

            verify(achievements).achievementLeaderboard(anyInt(), any(), any());
            verifyNoInteractions(statistics);
        }

        /**
         * And it returns before the category whitelist, so a second word is ignored rather than
         * refused: the one place the two whitelists do not both apply.
         */
        @Test
        void achievementsIgnoresACategoryRatherThanRefusingIt() {
            PlayerMock player = join("Understudy1");
            achievementBoardOf(List.of());

            run(player, "achievements", "not_a_category");

            verify(achievements).achievementLeaderboard(anyInt(), any(), any());
        }
    }

    @Nested
    class Refusals {

        @Test
        void anUnknownScopeIsRefusedAndListsTheRealOnes() {
            PlayerMock player = join("Understudy1");

            run(player, "trios");

            String said = screenOf(player);
            assertTrue(said.contains("trios"), said);
            assertTrue(said.contains("is not a valid scope"), said);
            verifyNoInteractions(statistics);
            verifyNoInteractions(achievements);
        }

        @Test
        void anUnknownCategoryIsRefusedWithItsOwnWording() {
            PlayerMock player = join("Understudy1");

            run(player, "solo", "most_handsome");

            String said = screenOf(player);
            assertTrue(said.contains("most_handsome"), said);
            assertTrue(said.contains("does not exist in leaderboard"), said);
            verifyNoInteractions(statistics);
        }

        /** The two lists are separate: a scope is not a category, and vice versa. */
        @Test
        void aScopeNameIsNotAcceptedAsACategory() {
            PlayerMock player = join("Understudy1");

            run(player, "solo", "duo");

            assertSaid(player, "does not exist in leaderboard");
            verifyNoInteractions(statistics);
        }

        @Test
        void aCategoryNameIsNotAcceptedAsAScope() {
            PlayerMock player = join("Understudy1");

            run(player, "games_won");

            assertSaid(player, "is not a valid scope");
            verifyNoInteractions(statistics);
        }

        @Test
        void aFailedLoadSaysSoRatherThanRenderingAnEmptyBoard() {
            PlayerMock player = join("Understudy1");
            soloBoardFails();

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("Could not load leaderboard"), said);
            assertFalse(said.contains("No entries yet"), "a failure is not an empty board:\n" + said);
        }
    }

    @Nested
    class TheBoard {

        @Test
        void theHeadingNamesTheScopeAndTheCategory() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of(row(1, "Understudy1", 5)));

            run(player, "solo", "games_won");

            String said = screenOf(player);
            assertTrue(said.contains("Leaderboard"), said);
            assertTrue(said.contains("Games Won"),
                    "the underscore becomes a space and each word is capitalised:\n" + said);
        }

        @Test
        void theTeamsBoardSaysSoInItsHeading() {
            PlayerMock player = join("Understudy1");
            teamsBoardOf(List.of());

            run(player, "teams");

            assertSaid(player, "Teams Leaderboard");
        }

        @Test
        void theDuoBoardSaysSoInItsHeading() {
            PlayerMock player = join("Understudy1");
            duoBoardOf(List.of());

            run(player, "duo");

            assertSaid(player, "Duo Leaderboard");
        }

        @Test
        void everyRowIsRankedNamedAndValued() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of(
                    row(1, "First", 300),
                    row(2, "Second", 200),
                    row(3, "Third", 100)));

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("First"), said);
            assertTrue(said.contains("Second"), said);
            assertTrue(said.contains("Third"), said);
            assertTrue(said.contains("300"), said);
            assertTrue(said.contains("100"), said);
        }

        /** The duo board is the only one whose row carries two players. */
        @Test
        void aDuoRowNamesBothHalvesOfThePair() {
            PlayerMock player = join("Understudy1");
            duoBoardOf(List.of(new DuoLeaderboardEntry(1,
                    new PlayerIdentity(UUID.randomUUID(), "Understudy1"),
                    new PlayerIdentity(UUID.randomUUID(), "Understudy2"), 42)));

            run(player, "duo");

            String said = screenOf(player);
            assertTrue(said.contains("Understudy1"), said);
            assertTrue(said.contains("Understudy2"), said);
            assertTrue(said.contains("42"), said);
        }

        /**
         * A row the service knows only by uuid still renders. The guard exists so one nameless
         * entry cannot take down the whole board.
         */
        @Test
        void aRowWithoutANameFallsBackToTheUuid() {
            PlayerMock player = join("Understudy1");
            UUID nameless = UUID.randomUUID();
            soloBoardOf(List.of(new LeaderboardEntry(1, new PlayerIdentity(nameless, null), 7)));

            run(player, "solo");

            assertSaid(player, nameless.toString().substring(0, 8));
        }

        @Test
        void aRowWithNoIdentityAtAllStillRenders() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of(new LeaderboardEntry(1, null, 7)));

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("?"), said);
            assertTrue(said.contains("7"), said);
        }

        @Test
        void anEmptyBoardSaysSoUnderItsHeading() {
            PlayerMock player = join("Understudy1");
            soloBoardOf(List.of());

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("Leaderboard"), "the heading is still drawn:\n" + said);
            assertTrue(said.contains("No entries yet"), said);
        }

        /** Only distance carries a unit. */
        @Test
        void onlyBlocksTravelledIsSuffixed() {
            PlayerMock travelled = join("Understudy1");
            soloBoardOf(List.of(row(1, "Understudy1", 500)));
            run(travelled, "solo", "blocks_travelled");
            assertSaid(travelled, "500 blocks");

            PlayerMock scored = join("Understudy2");
            soloBoardOf(List.of(row(1, "Understudy1", 500)));
            run(scored, "solo", "highest_score");
            assertFalse(screenOf(scored).contains("500 blocks"));
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void theFirstArgumentOffersEveryScope() {
            PlayerMock player = join("Understudy1");

            assertEquals(List.of("solo", "duo", "teams", "achievements"),
                    command.onTabComplete(player, "top", new String[]{""}));
        }

        @Test
        void theSecondArgumentOffersEveryCategory() {
            PlayerMock player = join("Understudy1");

            List<String> offered = command.onTabComplete(player, "top", new String[]{"solo", ""});

            assertTrue(offered.contains("highest_score"), offered.toString());
            assertTrue(offered.contains("blocks_travelled"), offered.toString());
        }

        /** Completion mirrors the dispatch: achievements takes no category, so offers none. */
        @Test
        void achievementsOffersNoCategory() {
            PlayerMock player = join("Understudy1");

            assertTrue(command.onTabComplete(player, "top", new String[]{"achievements", ""})
                    .isEmpty());
        }

        @Test
        void anUnknownScopeOffersNoCategoryEither() {
            PlayerMock player = join("Understudy1");

            assertTrue(command.onTabComplete(player, "top", new String[]{"trios", ""}).isEmpty());
        }

        @Test
        void thereIsNothingToCompleteAfterTheCategory() {
            PlayerMock player = join("Understudy1");

            assertTrue(command.onTabComplete(player, "top",
                    new String[]{"solo", "games_won", ""}).isEmpty());
        }
    }
}
