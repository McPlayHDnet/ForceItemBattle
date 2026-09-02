package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandStats;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.model.RarityCounts;
import forceitembattle.model.stats.ItemCount;
import forceitembattle.model.stats.PlayerIdentity;
import forceitembattle.model.stats.StatsView;
import forceitembattle.model.stats.TeamMemberStats;
import forceitembattle.service.FIBServiceClient;
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
 * {@code /stats}: which row it asks for, who it is allowed to ask for, and what the screen says.
 *
 * <p>Four subcommand trees over one renderer. Three things here matter more than the rest:
 *
 * <ul>
 *   <li><b>{@code reset} is op-gated inside the switch</b>, through {@code requireOp(player,
 *       Runnable)} rather than a declared {@link Precondition}, because the gate hangs off
 *       {@code args[0]}. It is the only thing between a non-op and another player's wiped stats,
 *       so it is pinned from both sides: refused, and nothing staged.</li>
 *   <li><b>The two-step confirm.</b> A staged reset is keyed by the admin who asked for it, and
 *       {@code confirmReset} removes it before it checks anything — so a confirm is spent whether
 *       or not it fired.</li>
 *   <li><b>Which UUID reaches the service.</b> {@code /stats duo a b} pairs the two names;
 *       {@code /stats duo b} pairs the caller with one. Getting the argument offset wrong renders
 *       someone else's screen under your own name, and nothing else would notice.</li>
 * </ul>
 *
 * <p>The service is mocked at {@link FibStatisticsClient}, the seam the command actually talks to:
 * it hands a view and two callbacks, so a test drives either arm without a round or a transport.
 */
class CommandStatsTest {

    private ServerMock server;
    private FibStatisticsClient helper;
    private CommandStats command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        FIBServiceClient service = mock(FIBServiceClient.class);
        this.helper = mock(FibStatisticsClient.class);
        ItemDifficultiesManager items = mock(ItemDifficultiesManager.class);

        when(plugin.getFibService()).thenReturn(service);
        when(service.statistics()).thenReturn(this.helper);
        when(plugin.getItemDifficultiesManager()).thenReturn(items);
        when(items.getUnicodeFromMaterial(anyBoolean(), any())).thenReturn("?");

        this.command = new CommandStats(plugin);
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }


    private PlayerMock join(String name) {
        return this.server.addPlayer(name);
    }

    private PlayerMock joinOp(String name) {
        PlayerMock player = join(name);
        player.setOp(true);
        return player;
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "stats", args);
    }

    private static StatsView view() {
        return view(null, List.of(), List.of());
    }

    private static StatsView view(Long teamsPlayedWith, List<ItemCount> topItems,
                                  List<TeamMemberStats> members) {
        return new StatsView(
                10, 4, 250,
                topItems,
                1337, 88, "Highest score", 6,
                new RarityCounts(3, 2, 1, 0, 0),
                5, 9, 2, 1,
                500_000,
                teamsPlayedWith,
                members);
    }

    /** Makes the next {@code soloStats} call hand {@code view} to its success arm. */
    private void soloStatsSucceedWith(StatsView view) {
        doAnswer(invocation -> {
            invocation.<Consumer<StatsView>>getArgument(1).accept(view);
            return null;
        }).when(this.helper).soloStats(any(), any(), any());
    }

    private void soloStatsFail() {
        doAnswer(invocation -> {
            invocation.<Consumer<Object>>getArgument(2).accept(null);
            return null;
        }).when(this.helper).soloStats(any(), any(), any());
    }

    private void combinedTeamStatsSucceedWith(StatsView view) {
        doAnswer(invocation -> {
            invocation.<Consumer<StatsView>>getArgument(1).accept(view);
            return null;
        }).when(this.helper).combinedTeamStats(any(), any(), any());
    }

    private void combinedTeamStatsFail() {
        doAnswer(invocation -> {
            invocation.<Consumer<Object>>getArgument(2).accept(null);
            return null;
        }).when(this.helper).combinedTeamStats(any(), any(), any());
    }

    private void teamStatsSucceedWith(StatsView view) {
        doAnswer(invocation -> {
            invocation.<Consumer<StatsView>>getArgument(2).accept(view);
            return null;
        }).when(this.helper).teamStats(any(), any(), any(), any());
    }

    private void teamStatsFail() {
        doAnswer(invocation -> {
            invocation.<Consumer<Object>>getArgument(3).accept(null);
            return null;
        }).when(this.helper).teamStats(any(), any(), any(), any());
    }


    @Nested
    class Usage {

        @Test
        void noArgumentsListsTheSubcommands() {
            PlayerMock player = join("Understudy1");

            run(player);

            String said = screenOf(player);
            assertTrue(said.contains("/stats solo"), said);
            assertTrue(said.contains("/stats team"), said);
            assertTrue(said.contains("/stats duo"), said);
        }

        @Test
        void anUnknownSubcommandListsThemToo() {
            PlayerMock player = join("Understudy1");

            run(player, "nonsense");

            assertSaid(player, "/stats solo");
            verifyNoInteractions(helper);
        }

        /** The reset lines are the only op-only part of the screen. */
        @Test
        void aNonOpIsNotToldThatResetExists() {
            PlayerMock player = join("Understudy1");

            run(player);

            assertFalse(screenOf(player).contains("/stats reset"),
                    "a non-op has no business being offered the reset");
        }

        @Test
        void anOpIsToldThatResetExists() {
            PlayerMock player = joinOp("Admin");

            run(player);

            assertSaid(player, "/stats reset solo");
        }

        /** The switch lowercases, so the casing a player types does not matter. */
        @Test
        void theSubcommandIsCaseInsensitive() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view());

            run(player, "SOLO");

            verify(helper).soloStats(eq(player.getUniqueId()), any(), any());
        }
    }

    @Nested
    class Solo {

        @Test
        void withNoNameItAsksForTheCallersOwnRow() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view());

            run(player, "solo");

            verify(helper).soloStats(eq(player.getUniqueId()), any(), any());
            assertSaid(player, "Solo Stats");
        }

        @Test
        void theHeaderNamesWhoTheScreenIsAbout() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view());

            run(player, "solo");

            assertSaid(player, "Understudy1");
        }

        @Test
        void anOnlineNameIsResolvedToThatPlayersRow() {
            PlayerMock player = join("Understudy1");
            PlayerMock other = join("Understudy2");
            soloStatsSucceedWith(view());

            run(player, "solo", "Understudy2");

            verify(helper).soloStats(eq(other.getUniqueId()), any(), any());
            assertSaid(player, "Understudy2");
        }

        @Test
        void anUnknownNameIsRefusedWithoutAskingTheService() {
            PlayerMock player = join("Understudy1");

            run(player, "solo", "NobodyAtAll");

            assertSaid(player, "was not found");
            verify(helper, never()).soloStats(any(), any(), any());
        }

        /** Own stats failing is a transport problem; someone else's is usually just an empty row. */
        @Test
        void aFailureOnOwnStatsReadsAsAFailure() {
            PlayerMock player = join("Understudy1");
            soloStatsFail();

            run(player, "solo");

            assertSaid(player, "Could not load your solo stats");
        }

        @Test
        void aFailureOnSomeoneElsesReadsAsNoStatsYet() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            soloStatsFail();

            run(player, "solo", "Understudy2");

            assertSaid(player, "has no solo stats yet");
        }
    }

    /**
     * The renderer, exercised once. Every subcommand goes through {@code sendStats}, so this is
     * asserted here rather than three times over.
     */
    @Nested
    class TheScreen {

        @Test
        void everyStatIsOnIt() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view());

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("Total items found"), said);
            assertTrue(said.contains("Travelled"), said);
            assertTrue(said.contains("Highest score"), said);
            assertTrue(said.contains("Back-to-Back streak"), said);
            assertTrue(said.contains("Games played"), said);
            assertTrue(said.contains("Games won"), said);
            assertTrue(said.contains("Win percentage"), said);
            assertTrue(said.contains("Deaths"), said);
            assertTrue(said.contains("Longest item streak"), said);
            assertTrue(said.contains("Wheel of Fortune uses"), said);
            assertTrue(said.contains("Antimatter teleports"), said);
            assertTrue(said.contains("Avg. items / game"), said);
            assertTrue(said.contains("Avg. time per item"), said);
        }

        /** The score label travels with the view, because solo and team disagree on it. */
        @Test
        void theScoreLabelComesFromTheView() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(new StatsView(1, 1, 1, List.of(), 0, 42, "Best round", 0,
                    RarityCounts.NONE, 0, 0, 0, 0, 0, null, List.of()));

            run(player, "solo");

            assertSaid(player, "Best round");
        }

        @Test
        void theDerivedAveragesAreComputedNotEchoed() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view());

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("40%"), "4 wins in 10 games is 40%:\n" + said);
            assertTrue(said.contains("25"), "250 items in 10 games is 25 each:\n" + said);
        }

        @Test
        void theRarityBlockIsSkippedWhenThereAreNone() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(new StatsView(1, 0, 0, List.of(), 0, 0, "Highest score", 0,
                    RarityCounts.NONE, 0, 0, 0, 0, 0, null, List.of()));

            run(player, "solo");

            assertFalse(screenOf(player).contains("Rarities"), "an empty rarity block is noise");
        }

        @Test
        void onlyTheRaritiesActuallyEarnedAreListed() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view());

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("Rare"), said);
            assertTrue(said.contains("Legendary"), said);
            assertFalse(said.contains("Extraordinary"), "a zero count is not a line:\n" + said);
        }

        @Test
        void theTopItemsAreNamedAndCounted() {
            PlayerMock player = join("Understudy1");
            soloStatsSucceedWith(view(null,
                    List.of(new ItemCount("diamond_sword", 12)), List.of()));

            run(player, "solo");

            String said = screenOf(player);
            assertTrue(said.contains("Diamond Sword"), "the material name is prettified:\n" + said);
            assertTrue(said.contains("12"), said);
        }

        /**
         * {@code teamsPlayedWith} is the one line the three views disagree about, so it is
         * nullable and only rendered when the view carries it.
         */
        @Test
        void teamsPlayedWithAppearsOnlyWhenTheViewHasIt() {
            PlayerMock soloScreen = join("Understudy1");
            soloStatsSucceedWith(view());
            run(soloScreen, "solo");
            assertFalse(screenOf(soloScreen).contains("Teams played with"));

            PlayerMock teamScreen = join("Understudy2");
            combinedTeamStatsSucceedWith(view(7L, List.of(), List.of()));
            run(teamScreen, "team");
            assertSaid(teamScreen, "Teams played with");
        }
    }

    @Nested
    class Team {

        @Test
        void withNoNameItAsksForTheCallersCombinedRow() {
            PlayerMock player = join("Understudy1");
            combinedTeamStatsSucceedWith(view());

            run(player, "team");

            verify(helper).combinedTeamStats(eq(player.getUniqueId()), any(), any());
            assertSaid(player, "Team Stats");
        }

        @Test
        void anOnlineNameIsResolved() {
            PlayerMock player = join("Understudy1");
            PlayerMock other = join("Understudy2");
            combinedTeamStatsSucceedWith(view());

            run(player, "team", "Understudy2");

            verify(helper).combinedTeamStats(eq(other.getUniqueId()), any(), any());
        }

        @Test
        void anUnknownNameIsRefusedWithoutAskingTheService() {
            PlayerMock player = join("Understudy1");

            run(player, "team", "NobodyAtAll");

            assertSaid(player, "was not found");
            verify(helper, never()).combinedTeamStats(any(), any(), any());
        }

        @Test
        void aFailureOnOwnStatsReadsAsAFailure() {
            PlayerMock player = join("Understudy1");
            combinedTeamStatsFail();

            run(player, "team");

            assertSaid(player, "Could not load your team stats");
        }

        @Test
        void aFailureOnSomeoneElsesReadsAsNoStatsYet() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            combinedTeamStatsFail();

            run(player, "team", "Understudy2");

            assertSaid(player, "has no team stats yet");
        }
    }

    @Nested
    class Duo {

        @Test
        void withNoTeammateItSaysHowToAskProperly() {
            PlayerMock player = join("Understudy1");

            run(player, "duo");

            assertSaid(player, "Usage: /stats duo");
            verifyNoInteractions(helper);
        }

        @Test
        void oneNamePairsTheCallerWithThatPlayer() {
            PlayerMock player = join("Understudy1");
            PlayerMock other = join("Understudy2");
            teamStatsSucceedWith(view());

            run(player, "duo", "Understudy2");

            verify(helper).teamStats(eq(player.getUniqueId()), eq(other.getUniqueId()), any(), any());
        }

        @Test
        void twoNamesPairThoseTwoAndNotTheCaller() {
            PlayerMock player = join("Understudy1");
            PlayerMock first = join("Understudy2");
            PlayerMock second = join("Understudy3");
            teamStatsSucceedWith(view());

            run(player, "duo", "Understudy2", "Understudy3");

            verify(helper).teamStats(eq(first.getUniqueId()), eq(second.getUniqueId()), any(), any());
        }

        @Test
        void theHeaderNamesBothSides() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            teamStatsSucceedWith(view());

            run(player, "duo", "Understudy2");

            String said = screenOf(player);
            assertTrue(said.contains("Duo Stats"), said);
            assertTrue(said.contains("Understudy1"), said);
            assertTrue(said.contains("Understudy2"), said);
        }

        @Test
        void anUnknownFirstNameIsRefused() {
            PlayerMock player = join("Understudy1");
            join("Understudy3");

            run(player, "duo", "NobodyAtAll", "Understudy3");

            assertSaid(player, "NobodyAtAll");
            verify(helper, never()).teamStats(any(), any(), any(), any());
        }

        @Test
        void anUnknownSecondNameIsRefused() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");

            run(player, "duo", "Understudy2", "NobodyAtAll");

            assertSaid(player, "NobodyAtAll");
            verify(helper, never()).teamStats(any(), any(), any(), any());
        }

        @Test
        void aFailureNamesBothSides() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            teamStatsFail();

            run(player, "duo", "Understudy2");

            String said = screenOf(player);
            assertTrue(said.contains("no duo stats yet"), said);
            assertTrue(said.contains("Understudy2"), said);
        }

        /** The contributions block is the only thing duo renders that the other two do not. */
        @Test
        void everyMembersContributionIsListed() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            teamStatsSucceedWith(view(null, List.of(), List.of(
                    new TeamMemberStats(new PlayerIdentity(UUID.randomUUID(), "Understudy1"), 30, 2, 400),
                    new TeamMemberStats(new PlayerIdentity(UUID.randomUUID(), "Understudy2"), 20, 1, 600))));

            run(player, "duo", "Understudy2");

            String said = screenOf(player);
            assertTrue(said.contains("Contributions"), said);
            assertTrue(said.contains("30 items"), said);
            assertTrue(said.contains("20 items"), said);
            assertTrue(said.contains("600 blocks"), said);
        }

        @Test
        void anEmptyContributionListIsNotAnEmptyHeading() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            teamStatsSucceedWith(view());

            run(player, "duo", "Understudy2");

            assertFalse(screenOf(player).contains("Contributions"));
        }

        /** A member the service knows only by UUID still renders, rather than blanking the row. */
        @Test
        void aMemberWithoutANameFallsBackToTheirUuid() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");
            UUID nameless = UUID.randomUUID();
            teamStatsSucceedWith(view(null, List.of(), List.of(
                    new TeamMemberStats(new PlayerIdentity(nameless, null), 5, 0, 0))));

            run(player, "duo", "Understudy2");

            assertSaid(player, nameless.toString().substring(0, 8));
        }
    }

    /**
     * The destructive half. {@code reset} is gated inside the switch rather than by a declared
     * precondition, which is the arrangement that has already been got backwards once in this
     * package.
     */
    @Nested
    class Reset {

        @Test
        void aNonOpIsRefused() {
            PlayerMock player = join("Understudy1");
            join("Victim");

            run(player, "reset", "solo", "Victim");

            assertSaid(player, "permission");
            verify(helper, never()).deleteSoloStatisticsAsync(any(), any(), any());
        }

        /**
         * And is refused <em>before</em> the request is staged, so the gate is not merely a
         * message: op the same player afterwards and there is still nothing to confirm.
         */
        @Test
        void aNonOpStagesNothingToConfirmLater() {
            PlayerMock player = join("Understudy1");
            join("Victim");

            run(player, "reset", "solo", "Victim");
            player.setOp(true);
            screenOf(player);
            run(player, "reset", "confirm");

            assertSaid(player, "no pending reset");
            verifyNoInteractions(helper);
        }

        @Test
        void anOpWithTooFewArgumentsIsShownTheForm() {
            PlayerMock admin = joinOp("Admin");

            run(admin, "reset", "solo");

            assertSaid(admin, "Usage: /stats reset");
        }

        @Test
        void aScopeThatIsNeitherSoloNorTeamIsShownTheForm() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");

            run(admin, "reset", "everything", "Victim");

            assertSaid(admin, "Usage: /stats reset");
            verifyNoInteractions(helper);
        }

        @Test
        void anUnknownTargetIsRefused() {
            PlayerMock admin = joinOp("Admin");

            run(admin, "reset", "solo", "NobodyAtAll");

            assertSaid(admin, "was not found");
            verifyNoInteractions(helper);
        }

        /** Staging is not deleting — the whole point of the two steps. */
        @Test
        void stagingWarnsAndDeletesNothing() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");

            run(admin, "reset", "solo", "Victim");

            String said = screenOf(admin);
            assertTrue(said.contains("about to reset"), said);
            assertTrue(said.contains("Victim"), said);
            assertTrue(said.contains("/stats reset confirm"), said);
            verifyNoInteractions(helper);
        }

        @Test
        void confirmingASoloResetDeletesTheSoloRow() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock victim = join("Victim");

            run(admin, "reset", "solo", "Victim");
            run(admin, "reset", "confirm");

            verify(helper).deleteSoloStatisticsAsync(eq(victim.getUniqueId()), any(), any());
            verify(helper, never()).deleteAllTeamStatisticsForPlayerAsync(any(), any(), any());
        }

        @Test
        void confirmingATeamResetDeletesEveryTeamRow() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock victim = join("Victim");

            run(admin, "reset", "team", "Victim");
            run(admin, "reset", "confirm");

            verify(helper).deleteAllTeamStatisticsForPlayerAsync(eq(victim.getUniqueId()), any(), any());
            verify(helper, never()).deleteSoloStatisticsAsync(any(), any(), any());
        }

        @Test
        void theScopeIsCaseInsensitive() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock victim = join("Victim");

            run(admin, "reset", "SOLO", "Victim");
            run(admin, "reset", "CONFIRM");

            verify(helper).deleteSoloStatisticsAsync(eq(victim.getUniqueId()), any(), any());
        }

        @Test
        void confirmingWithNothingStagedIsRefused() {
            PlayerMock admin = joinOp("Admin");

            run(admin, "reset", "confirm");

            assertSaid(admin, "no pending reset");
            verifyNoInteractions(helper);
        }

        /** A confirm is spent when it is used, so a stray second one cannot re-fire it. */
        @Test
        void aConfirmIsSpentOnce() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");

            run(admin, "reset", "solo", "Victim");
            run(admin, "reset", "confirm");
            screenOf(admin);
            run(admin, "reset", "confirm");

            assertSaid(admin, "no pending reset");
            verify(helper).deleteSoloStatisticsAsync(any(), any(), any());
        }

        /** Staged resets are keyed per admin, not globally. */
        @Test
        void oneAdminCannotConfirmAnothersStagedReset() {
            PlayerMock stager = joinOp("Admin");
            PlayerMock bystander = joinOp("OtherAdmin");
            join("Victim");

            run(stager, "reset", "solo", "Victim");
            run(bystander, "reset", "confirm");

            assertSaid(bystander, "no pending reset");
            verifyNoInteractions(helper);
        }

        /** Re-staging replaces rather than queues: the confirm resolves the most recent request. */
        @Test
        void restagingReplacesTheEarlierRequest() {
            PlayerMock admin = joinOp("Admin");
            join("FirstVictim");
            PlayerMock second = join("SecondVictim");

            run(admin, "reset", "solo", "FirstVictim");
            run(admin, "reset", "team", "SecondVictim");
            run(admin, "reset", "confirm");

            verify(helper).deleteAllTeamStatisticsForPlayerAsync(eq(second.getUniqueId()), any(), any());
            verify(helper, never()).deleteSoloStatisticsAsync(any(), any(), any());
        }

        @Test
        void aSuccessfulResetSaysSo() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");
            doAnswer(invocation -> {
                invocation.<Runnable>getArgument(1).run();
                return null;
            }).when(helper).deleteSoloStatisticsAsync(any(), any(), any());

            run(admin, "reset", "solo", "Victim");
            screenOf(admin);
            run(admin, "reset", "confirm");

            // Colour codes split the sentence, so the name and the scope are matched separately.
            String said = screenOf(admin);
            assertTrue(said.contains("Reset"), said);
            assertTrue(said.contains("Victim"), said);
            assertTrue(said.contains("solo stats"), said);
        }

        @Test
        void aFailedResetSaysSoToo() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");
            doAnswer(invocation -> {
                invocation.<Consumer<Object>>getArgument(2).accept(null);
                return null;
            }).when(helper).deleteSoloStatisticsAsync(any(), any(), any());

            run(admin, "reset", "solo", "Victim");
            screenOf(admin);
            run(admin, "reset", "confirm");

            assertSaid(admin, "Could not reset");
        }
    }

    /**
     * Tab completion, which is the other place the op gate is expressed — and expressed
     * separately, so it can drift from the switch.
     */
    @Nested
    class TabCompletion {

        @Test
        void theFirstArgumentOffersTheSubcommands() {
            PlayerMock player = join("Understudy1");

            List<String> offered = command.onTabComplete(player, "stats", new String[]{""});

            assertEquals(List.of("solo", "team", "duo"), offered);
        }

        @Test
        void onlyAnOpIsOfferedReset() {
            PlayerMock admin = joinOp("Admin");

            assertTrue(command.onTabComplete(admin, "stats", new String[]{""}).contains("reset"));
        }

        @Test
        void theSecondArgumentOffersOnlinePlayers() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");

            List<String> offered = command.onTabComplete(player, "stats", new String[]{"solo", ""});

            assertTrue(offered.contains("Understudy1"), offered.toString());
            assertTrue(offered.contains("Understudy2"), offered.toString());
        }

        @Test
        void afterResetTheSecondArgumentOffersScopesInstead() {
            PlayerMock admin = joinOp("Admin");

            List<String> offered = command.onTabComplete(admin, "stats", new String[]{"reset", ""});

            assertEquals(List.of("solo", "team", "confirm"), offered);
        }

        /** A non-op typing "reset" is offered nothing rather than the scopes. */
        @Test
        void aNonOpGetsNothingAfterTypingReset() {
            PlayerMock player = join("Understudy1");

            assertTrue(command.onTabComplete(player, "stats", new String[]{"reset", ""}).isEmpty());
        }

        @Test
        void duoOffersASecondPlayer() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");

            List<String> offered =
                    command.onTabComplete(player, "stats", new String[]{"duo", "Understudy2", ""});

            assertTrue(offered.contains("Understudy1"), offered.toString());
        }

        @Test
        void aNonOpIsOfferedNoResetTarget() {
            PlayerMock player = join("Understudy1");
            join("Victim");

            assertTrue(command.onTabComplete(player, "stats", new String[]{"reset", "solo", ""})
                    .isEmpty());
        }
    }
}
