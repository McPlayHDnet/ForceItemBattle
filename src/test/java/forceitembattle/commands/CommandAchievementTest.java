package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementStorage;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.global.GlobalStat;
import forceitembattle.achievements.global.GlobalStats;
import forceitembattle.achievements.global.GlobalStatsLoader;
import forceitembattle.commands.player.CommandAchievement;
import forceitembattle.manager.AchievementManager;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /achievements}: who may grant, who may revoke, and who the subcommand is about.
 *
 * <p>Six subcommands, three of which write. Like {@code /stats reset}, those three are gated
 * <em>inside</em> the switch through {@code requireOp(player, Runnable)} rather than by a declared
 * {@link Precondition}, because the gate hangs off {@code args[0]} and a command-level declaration
 * cannot see it. Three separate call sites is three chances to leave one off, and leaving one off
 * looks exactly like a subcommand that correctly has no gate. All three are pinned here, from both
 * sides: refused, and the storage untouched.
 *
 * <p>The other thing worth pinning is the argument arithmetic on {@code progress}, which is the one
 * subcommand whose meaning shifts with the count: two arguments name an achievement and mean
 * yourself, three name a player and an achievement. An off-by-one reads the achievement name out
 * of the player slot and reports "does not exist" for a real achievement.
 *
 * <p>{@code list} is verified only as far as the storage load it requests. What its callback opens
 * is an {@code AchievementCategoryInventory}, and building one needs {@code ItemStack} — the
 * headless wall that {@code HeadlessBoundaryTest} pins. Leaving the callback unrun is what keeps
 * this test on the near side of it.
 */
class CommandAchievementTest {

    private ServerMock server;
    private AchievementManager achievements;
    private AchievementStorage storage;
    private GlobalStatsLoader globalStats;
    private CommandAchievement command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.achievements = mock(AchievementManager.class);
        this.storage = mock(AchievementStorage.class);
        this.globalStats = mock(GlobalStatsLoader.class);

        when(plugin.getAchievementManager()).thenReturn(this.achievements);
        when(this.achievements.getAchievementStorage()).thenReturn(this.storage);
        when(this.achievements.getGlobalStatsLoader()).thenReturn(this.globalStats);

        this.command = new CommandAchievement(plugin);
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

    private PlayerMock joinOp(String name) {
        PlayerMock player = join(name);
        player.setOp(true);
        return player;
    }

    private void run(PlayerMock player, String... args) {
        this.command.onCommand(player, null, "achievements", args);
    }

    /** Every write path goes through one of these three, so "nothing happened" means all three. */
    private void assertNothingWasWritten() {
        verify(this.storage, never()).addAchievement(any(), any());
        verify(this.storage, never()).removeAchievement(any(), any());
        verify(this.storage, never()).resetPlayerAchievements(any());
    }

    private void globalStatsOf(Map<GlobalStat, Long> values) {
        Map<GlobalStat, Long> filled = new EnumMap<>(GlobalStat.class);
        for (GlobalStat stat : GlobalStat.values()) {
            filled.put(stat, values.getOrDefault(stat, 0L));
        }
        doAnswer(invocation -> {
            invocation.<Consumer<GlobalStats>>getArgument(1).accept(new GlobalStats(filled));
            return null;
        }).when(this.globalStats).load(any(), any());
    }

    // --- the tests --------------------------------------------------------------------------

    @Nested
    class Dispatch {

        @Test
        void noArgumentsListsTheSubcommands() {
            PlayerMock player = join("Understudy1");

            run(player);

            assertSaid(player, "Usage: /achievements");
            verifyNoInteractions(storage);
        }

        @Test
        void anUnknownSubcommandIsRefused() {
            PlayerMock player = join("Understudy1");

            run(player, "nonsense");

            assertSaid(player, "Unknown subcommand");
            verifyNoInteractions(storage);
        }

        @Test
        void theSubcommandIsCaseInsensitive() {
            PlayerMock player = join("Understudy1");

            run(player, "LIST");

            verify(storage).loadPlayer(eq(player.getUniqueId()), any());
        }
    }

    /**
     * The read-only half. These three are open to everyone on purpose, so a non-op reaching them
     * is the correct behaviour rather than a hole.
     */
    @Nested
    class Reading {

        @Test
        void listWithNoNameLoadsTheCallersOwnRow() {
            PlayerMock player = join("Understudy1");

            run(player, "list");

            verify(storage).loadPlayer(eq(player.getUniqueId()), any());
        }

        @Test
        void listWithANameLoadsThatPlayersRow() {
            PlayerMock player = join("Understudy1");
            PlayerMock other = join("Understudy2");

            run(player, "list", "Understudy2");

            verify(storage).loadPlayer(eq(other.getUniqueId()), any());
        }

        /** Reading is not op-gated: a non-op looking at their own achievements is the normal case. */
        @Test
        void aNonOpMayList() {
            PlayerMock player = join("Understudy1");

            run(player, "list");

            verify(storage).loadPlayer(any(), any());
            assertFalse(screenOf(player).contains("permission"));
        }

        @Test
        void globalWithNoNameLoadsTheCallersOwnStats() {
            PlayerMock player = join("Understudy1");
            globalStatsOf(Map.of());

            run(player, "global");

            verify(globalStats).load(eq(player.getUniqueId()), any());
        }

        @Test
        void globalRendersEveryStatUnderTheSubjectsName() {
            PlayerMock player = join("Understudy1");
            globalStatsOf(Map.of(GlobalStat.GAMES_PLAYED, 42L, GlobalStat.GAMES_WON, 7L));

            run(player, "global");

            String said = screenOf(player);
            assertTrue(said.contains("Global Stats"), said);
            assertTrue(said.contains("Understudy1"), said);
            assertTrue(said.contains("games played"), said);
            assertTrue(said.contains("42"), said);
            assertTrue(said.contains("7"), said);
        }

        /**
         * Both read paths check {@code isOnline()} inside the callback, because the load is
         * asynchronous and the requester can log off between asking and being answered.
         */
        @Test
        void aRequesterWhoLoggedOffIsNotRenderedTo() {
            PlayerMock player = join("Understudy1");
            player.disconnect();
            globalStatsOf(Map.of(GlobalStat.GAMES_PLAYED, 42L));

            run(player, "global");

            assertFalse(screenOf(player).contains("Global Stats"),
                    "the callback must notice the requester is gone");
        }
    }

    @Nested
    class Progress {

        @Test
        void twoArgumentsMeanYourOwnProgressOnThatAchievement() {
            PlayerMock player = join("Understudy1");
            when(achievements.describeProgress(any(), any())).thenReturn("12/40");

            run(player, "progress", "ITEM_COLLECTOR");

            verify(achievements).describeProgress(eq(player.getUniqueId()),
                    eq(Achievements.ITEM_COLLECTOR));
        }

        /** Three arguments shift the achievement into the last slot, and name someone else. */
        @Test
        void threeArgumentsMeanSomeoneElsesProgress() {
            PlayerMock player = join("Understudy1");
            PlayerMock other = join("Understudy2");
            when(achievements.describeProgress(any(), any())).thenReturn("3/40");

            run(player, "progress", "Understudy2", "ITEM_COLLECTOR");

            verify(achievements).describeProgress(eq(other.getUniqueId()),
                    eq(Achievements.ITEM_COLLECTOR));
        }

        /** Live progress is held in memory for the session, so an offline player has none to read. */
        @Test
        void anOfflinePlayerCannotBeInspected() {
            PlayerMock player = join("Understudy1");

            run(player, "progress", "NobodyAtAll", "ITEM_COLLECTOR");

            assertSaid(player, "must be online");
            verify(achievements, never()).describeProgress(any(), any());
        }

        @Test
        void anUnknownAchievementIsRefused() {
            PlayerMock player = join("Understudy1");

            run(player, "progress", "NOT_AN_ACHIEVEMENT");

            assertSaid(player, "does not exist");
            verify(achievements, never()).describeProgress(any(), any());
        }

        @Test
        void theAchievementNameIsCaseInsensitive() {
            PlayerMock player = join("Understudy1");
            when(achievements.describeProgress(any(), any())).thenReturn("0/40");

            run(player, "progress", "item_collector");

            verify(achievements).describeProgress(any(), eq(Achievements.ITEM_COLLECTOR));
        }

        @Test
        void neitherNoArgumentsNorFourIsAValidForm() {
            PlayerMock player = join("Understudy1");

            run(player, "progress");
            assertSaid(player, "Usage: /achievements progress");

            run(player, "progress", "a", "b", "c");
            assertSaid(player, "Usage: /achievements progress");
        }

        @Test
        void theReportCarriesTheTitleTheDescriptionAndTheProgress() {
            PlayerMock player = join("Understudy1");
            when(achievements.describeProgress(any(), any())).thenReturn("12/40");
            when(storage.hasAchievement(any(), any())).thenReturn(false);

            run(player, "progress", "ITEM_COLLECTOR");

            String said = screenOf(player);
            assertTrue(said.contains(Achievements.ITEM_COLLECTOR.getTitle()), said);
            assertTrue(said.contains(Achievements.ITEM_COLLECTOR.getDescription()), said);
            assertTrue(said.contains("12/40"), said);
            assertTrue(said.contains("In progress"), said);
        }

        @Test
        void anAlreadyEarnedAchievementReadsAsUnlocked() {
            PlayerMock player = join("Understudy1");
            when(achievements.describeProgress(any(), any())).thenReturn("40/40");
            when(storage.hasAchievement(any(), any())).thenReturn(true);

            run(player, "progress", "ITEM_COLLECTOR");

            assertSaid(player, "Unlocked");
        }

        /** Progress is open to everyone, like the other two read paths. */
        @Test
        void aNonOpMayInspectTheirOwnProgress() {
            PlayerMock player = join("Understudy1");
            when(achievements.describeProgress(any(), any())).thenReturn("1/40");

            run(player, "progress", "ITEM_COLLECTOR");

            verify(achievements).describeProgress(any(), any());
        }
    }

    /**
     * The op gate, asserted once per gated subcommand. Three separate {@code requireOp} call sites
     * means the gate can go missing from one of them without the other two noticing.
     */
    @Nested
    class TheOpGate {

        @Test
        void aNonOpMayNotGrant() {
            PlayerMock player = join("Understudy1");
            join("Victim");

            run(player, "grant", "Victim", "ITEM_COLLECTOR");

            assertSaid(player, "permission");
            assertNothingWasWritten();
        }

        @Test
        void aNonOpMayNotRevoke() {
            PlayerMock player = join("Understudy1");
            join("Victim");

            run(player, "revoke", "Victim", "ITEM_COLLECTOR");

            assertSaid(player, "permission");
            assertNothingWasWritten();
        }

        @Test
        void aNonOpMayNotReset() {
            PlayerMock player = join("Understudy1");
            join("Victim");

            run(player, "reset", "Victim");

            assertSaid(player, "permission");
            assertNothingWasWritten();
        }
    }

    @Nested
    class Writing {

        @Test
        void anOpCanGrantAnAchievement() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock target = join("Victim");

            run(admin, "grant", "Victim", "ITEM_COLLECTOR");

            verify(storage).addAchievement(target.getUniqueId(), Achievements.ITEM_COLLECTOR);
            assertSaid(admin, "granted");
        }

        @Test
        void anOpCanRevokeAnAchievement() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock target = join("Victim");

            run(admin, "revoke", "Victim", "ITEM_COLLECTOR");

            verify(storage).removeAchievement(target.getUniqueId(), Achievements.ITEM_COLLECTOR);
            assertSaid(admin, "revoked");
        }

        @Test
        void anOpCanResetEverythingAPlayerHas() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock target = join("Victim");

            run(admin, "reset", "Victim");

            verify(storage).resetPlayerAchievements(target.getUniqueId());
            assertSaid(admin, "reset all achievements");
        }

        @Test
        void theAchievementNameIsCaseInsensitiveOnWritesToo() {
            PlayerMock admin = joinOp("Admin");
            PlayerMock target = join("Victim");

            run(admin, "grant", "Victim", "item_collector");

            verify(storage).addAchievement(target.getUniqueId(), Achievements.ITEM_COLLECTOR);
        }

        @Test
        void anUnknownAchievementIsRefusedBeforeAnythingIsWritten() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");

            run(admin, "grant", "Victim", "NOT_AN_ACHIEVEMENT");

            assertSaid(admin, "does not exist");
            assertNothingWasWritten();
        }

        @Test
        void grantAndRevokeBothRequireExactlyThreeArguments() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");

            run(admin, "grant", "Victim");
            assertSaid(admin, "Usage: /achievements grant");

            run(admin, "revoke", "Victim");
            assertSaid(admin, "Usage: /achievements revoke");

            assertNothingWasWritten();
        }

        @Test
        void resetRequiresExactlyTwo() {
            PlayerMock admin = joinOp("Admin");
            join("Victim");

            run(admin, "reset");
            assertSaid(admin, "Usage: /achievements reset");

            run(admin, "reset", "Victim", "ITEM_COLLECTOR");
            assertSaid(admin, "Usage: /achievements reset");

            assertNothingWasWritten();
        }

        /**
         * Current behaviour, recorded rather than endorsed. The write paths resolve their target
         * with {@code Bukkit.getOfflinePlayer(String)}, which never returns null and never fails:
         * a name nobody has ever used still yields a uuid, so a typo writes a row against a player
         * who does not exist and the admin is told it worked. {@code /stats reset} does not have
         * this shape — it resolves through {@code getOfflinePlayerIfCached} and refuses an unknown
         * name. If that stricter form is adopted here, this test is the one to change.
         */
        @Test
        void anUnknownTargetIsNotRefusedOnTheWritePaths() {
            PlayerMock admin = joinOp("Admin");

            run(admin, "grant", "NobodyHasEverUsedThisName", "ITEM_COLLECTOR");

            verify(storage).addAchievement(
                    eq(Bukkit.getOfflinePlayer("NobodyHasEverUsedThisName").getUniqueId()),
                    eq(Achievements.ITEM_COLLECTOR));
        }
    }

    /**
     * Completion carries its own copy of the op gate, expressed separately from the switch, so it
     * can drift from what the command will actually let you do.
     */
    @Nested
    class TabCompletion {

        @Test
        void aNonOpIsOfferedOnlyTheReadSubcommands() {
            PlayerMock player = join("Understudy1");

            List<String> offered = command.onTabComplete(player, "achievements", new String[]{""});

            assertTrue(offered.containsAll(List.of("list", "progress", "global")), offered.toString());
            assertFalse(offered.contains("grant"), offered.toString());
            assertFalse(offered.contains("revoke"), offered.toString());
            assertFalse(offered.contains("reset"), offered.toString());
        }

        @Test
        void anOpIsOfferedTheWriteSubcommandsToo() {
            PlayerMock admin = joinOp("Admin");

            List<String> offered = command.onTabComplete(admin, "achievements", new String[]{""});

            assertTrue(offered.containsAll(List.of("grant", "revoke", "reset")), offered.toString());
        }

        @Test
        void whatIsAlreadyTypedNarrowsTheOffer() {
            PlayerMock player = join("Understudy1");

            List<String> offered =
                    command.onTabComplete(player, "achievements", new String[]{"gl"});

            assertTrue(offered.contains("global"), offered.toString());
            assertFalse(offered.contains("list"), offered.toString());
        }

        /** The second argument of {@code progress} is an achievement, not a player. */
        @Test
        void progressOffersAchievementsWhereTheOthersOfferPlayers() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");

            assertTrue(command.onTabComplete(player, "achievements", new String[]{"progress", ""})
                    .contains("ITEM_COLLECTOR"));
            assertTrue(command.onTabComplete(player, "achievements", new String[]{"list", ""})
                    .contains("Understudy2"));
        }

        @Test
        void aNonOpIsOfferedNoTargetForAGatedSubcommand() {
            PlayerMock player = join("Understudy1");
            join("Understudy2");

            assertTrue(command.onTabComplete(player, "achievements", new String[]{"grant", ""})
                    .isEmpty());
        }

        @Test
        void anOpIsOfferedAnAchievementInTheThirdSlot() {
            PlayerMock admin = joinOp("Admin");

            assertTrue(command.onTabComplete(admin, "achievements",
                    new String[]{"grant", "Victim", ""}).contains("ITEM_COLLECTOR"));
        }

        /** Reset takes no achievement, so its third slot offers nothing even for an op. */
        @Test
        void resetOffersNothingInTheThirdSlot() {
            PlayerMock admin = joinOp("Admin");

            assertTrue(command.onTabComplete(admin, "achievements",
                    new String[]{"reset", "Victim", ""}).isEmpty());
        }

        @Test
        void aNonOpIsOfferedNoAchievementInTheThirdSlotEither() {
            PlayerMock player = join("Understudy1");

            assertTrue(command.onTabComplete(player, "achievements",
                    new String[]{"grant", "Victim", ""}).isEmpty());
        }
    }
}
