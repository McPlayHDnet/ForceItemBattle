package forceitembattle.commands;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.OP_WHEN_EVENT;
import static forceitembattle.commands.Precondition.PARTICIPANT;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import static forceitembattle.commands.CommandTestSupport.contextWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.model.GameState;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link CustomCommand}: the rules every command inherits. Who may run a command, and from where, is
 * decided here and nowhere else — a regression unlocks or breaks all of them at once.
 *
 * <p>Commands <em>declare</em> their gates. Checking them by hand once let {@code /skip} invert its
 * {@code if} and run its whole body for non-ops only; {@link DeclaredGates} is the table that catches
 * that.
 */
class CustomCommandTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Records what reached it, so the test can ask which path ran. */
    private static final class ProbeCommand extends CustomCommand {

        private final List<String> ran = new ArrayList<>();
        private final List<Precondition> gates;

        ProbeCommand(Precondition... gates) {
            super("probe");
            this.gates = List.of(gates);
        }

        @Override
        protected List<Precondition> preconditions() {
            return this.gates;
        }

        @Override
        public void onPlayerCommand(Player player, String label, String[] args) {
            this.ran.add("player");
        }
    }

    /** A probe that also accepts the console, the way {@code /start} does. */
    private static final class ConsoleFriendlyCommand extends CustomCommand {

        private final List<String> ran = new ArrayList<>();
        private final List<Precondition> gates;

        ConsoleFriendlyCommand(Precondition... gates) {
            super("consoleprobe");
            this.gates = List.of(gates);
        }

        @Override
        protected List<Precondition> preconditions() {
            return this.gates;
        }

        @Override
        public void onPlayerCommand(Player player, String label, String[] args) {
            this.ran.add("player");
        }

        @Override
        public void onConsoleCommand(CommandSender sender, String label, String[] args) {
            this.ran.add("console");
        }
    }

    private ProbeCommand probe(CommandContext context, Precondition... gates) {
        ProbeCommand command = new ProbeCommand(gates);
        command.setContext(context);
        return command;
    }

    @Nested
    class Routing {

        @Test
        void aPlayerWithNoGatesReachesThePlayerPath() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()));
            PlayerMock player = server.addPlayer("Understudy1");

            command.onCommand(player, null, "probe", new String[0]);

            assertEquals(List.of("player"), command.ran);
        }

        /**
         * The console is refused by default. It matters that this is the default rather than the
         * rule: {@code /start} overrides it deliberately so a server owner — or the round-test
         * harness over RCON — can start a round with no player online.
         */
        @Test
        void theConsoleIsRefusedByDefault() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()));
            ConsoleCommandSenderMock console = server.getConsoleSender();

            command.onCommand(console, null, "probe", new String[0]);

            assertTrue(command.ran.isEmpty(), "the player path must not run for the console");
            console.assertSaid("This command can only be executed by a player");
        }

        @Test
        void onCommandAlwaysReportsHandled() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()), OP);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(false);

            assertTrue(command.onCommand(player, null, "probe", new String[0]),
                    "returning false would make Bukkit print the plugin.yml usage on top of ours");
        }
    }

    @Nested
    class Gates {

        @Test
        void anOpPassesTheOpGate() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()), OP);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(true);

            command.onCommand(player, null, "probe", new String[0]);

            assertEquals(List.of("player"), command.ran);
        }

        /** The regression that motivated the whole change: the body must not run for a non-op. */
        @Test
        void aNonOpIsRefusedAndTheBodyDoesNotRun() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()), OP);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(false);

            command.onCommand(player, null, "probe", new String[0]);

            assertTrue(command.ran.isEmpty(), "an inverted gate would run the body here");
            player.assertSaid("§cYou don't have permission to use this command.");
        }

        /** The console is implicitly op, so an op gate must not lock {@code /start} out of RCON. */
        @Test
        void theOpGatePassesForTheConsole() {
            ConsoleFriendlyCommand command = new ConsoleFriendlyCommand(OP);
            command.setContext(contextWith(GameState.PRE_GAME, new Roster()));

            command.onCommand(server.getConsoleSender(), null, "consoleprobe", new String[0]);

            assertEquals(List.of("console"), command.ran);
        }

        @Test
        void opWhenEventIsOpenWhileTheEventSettingIsOff() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()), OP_WHEN_EVENT);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(false);

            command.onCommand(player, null, "probe", new String[0]);

            assertEquals(List.of("player"), command.ran);
        }

        @Test
        void opWhenEventClosesOnceTheEventSettingIsOn() {
            ProbeCommand command = probe(
                    contextWith(GameState.PRE_GAME, new Roster(), GameSetting.EVENT), OP_WHEN_EVENT);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(false);

            command.onCommand(player, null, "probe", new String[0]);

            assertTrue(command.ran.isEmpty());
            player.assertSaid("§cYou don't have permission to use this command.");
        }

        @Test
        void aSettingGateRefusesWithItsOwnWording() {
            ProbeCommand command = probe(contextWith(GameState.MID_GAME, new Roster()),
                    Precondition.setting(GameSetting.TEAM, "<red>Teams are not enabled!"));
            PlayerMock player = server.addPlayer("Understudy1");

            command.onCommand(player, null, "probe", new String[0]);

            assertTrue(command.ran.isEmpty());
            player.assertSaid("§cTeams are not enabled!");
        }

        /** The console holds no roster entry, so it can never be a participant. */
        @Test
        void participantRefusesTheConsole() {
            ConsoleFriendlyCommand command = new ConsoleFriendlyCommand(PARTICIPANT);
            command.setContext(contextWith(GameState.MID_GAME, new Roster()));

            command.onCommand(server.getConsoleSender(), null, "consoleprobe", new String[0]);

            assertTrue(command.ran.isEmpty(), "a console sender is not playing");
        }

        /** Absent and spectating are one answer — see CONTEXT.md § Roster. */
        @Test
        void participantRefusesAPlayerWithNoRosterEntry() {
            ProbeCommand command = probe(contextWith(GameState.MID_GAME, new Roster()), PARTICIPANT);
            PlayerMock player = server.addPlayer("Understudy1");

            command.onCommand(player, null, "probe", new String[0]);

            assertTrue(command.ran.isEmpty());
            player.assertSaid("§cYou are not playing.");
        }
    }

    @Nested
    class Ordering {

        /** The list is ordered and the first failure is what the sender is told. */
        @Test
        void theFirstFailingGateIsTheOneReported() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()), OP, ROUND_RUNNING);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(false);

            command.onCommand(player, null, "probe", new String[0]);

            player.assertSaid("§cYou don't have permission to use this command.");
        }

        @Test
        void aLaterGateReportsOnceTheEarlierOneHolds() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()), OP, ROUND_RUNNING);
            PlayerMock player = server.addPlayer("Understudy1");
            player.setOp(true);

            command.onCommand(player, null, "probe", new String[0]);

            player.assertSaid("§cThe game is not running. Start it first with /start");
        }

        @Test
        void refusingOverridesTheWordingButNotTheCondition() {
            ProbeCommand command = probe(contextWith(GameState.PRE_GAME, new Roster()),
                    ROUND_RUNNING.refusing("<red>The timer is already paused."));
            PlayerMock player = server.addPlayer("Understudy1");

            command.onCommand(player, null, "probe", new String[0]);

            assertTrue(command.ran.isEmpty());
            player.assertSaid("§cThe timer is already paused.");
        }
    }

    /**
     * What every gated command declares, pinned.
     *
     * <p>The evaluator above is small and provably right. The risk is the sixteen hand-transcribed
     * declarations — done on files where one gate was already inverted — so those are compared
     * against a table rather than reviewed by eye. Same reasoning as {@code CrossRepoContractTest}:
     * pin the thing that can silently drift.
     *
     * <p>A command that loses a gate, gains one, or has them reordered fails here.
     */
    @Nested
    class DeclaredGates {

        private static String labelsOf(CustomCommand command) {
            return String.join(", ", command.declaredPreconditions().stream().map(Precondition::label).toList());
        }

        private void assertGates(CustomCommand command, String expected) {
            assertEquals(expected, labelsOf(command), command.getName() + " declares different gates");
        }

        // Only the declaration is under test here, and preconditions() reads no dependency,
        // so every command is built with nulls rather than a graph of mocks.

        @Test
        void theAdminCommands() {
            assertGates(new forceitembattle.commands.admin.CommandForceItem(null, null, null, null, null), "OP, ROUND_RUNNING, PARTICIPANT");
            assertGates(new forceitembattle.commands.admin.CommandForceTeam(null, null), "OP, setting(TEAM), PRE_GAME");
            assertGates(new forceitembattle.commands.admin.CommandItems(null), "OP");
            assertGates(new forceitembattle.commands.admin.CommandRandomEvent(null), "OP, ROUND_RUNNING");
            assertGates(new forceitembattle.commands.admin.CommandReset(null, null), "OP");
            assertGates(new forceitembattle.commands.admin.CommandSettings(null, null), "OP");
            assertGates(new forceitembattle.commands.admin.CommandSkip(null, null, null), "OP, ROUND_RUNNING");
            assertGates(new forceitembattle.commands.admin.CommandStart(null, null, null, null, null, null, null), "OP");
            assertGates(new forceitembattle.commands.admin.CommandStopTimer(null), "OP, ROUND_RUNNING");
        }

        @Test
        void thePlayerCommands() {
            assertGates(new forceitembattle.commands.player.CommandBp(null), "ROUND_RUNNING, setting(BACKPACK)");
            assertGates(new forceitembattle.commands.player.CommandFixSkips(null, null), "ROUND_RUNNING");
            assertGates(new forceitembattle.commands.player.CommandPause(null), "OP_WHEN_EVENT, ROUND_RUNNING");
            assertGates(new forceitembattle.commands.player.CommandPosition(null, null), "OP_WHEN_EVENT, setting(POSITIONS)");
            assertGates(new forceitembattle.commands.player.CommandResume(null), "OP_WHEN_EVENT, PAUSED");
            assertGates(new forceitembattle.commands.player.CommandTeams(null, null), "setting(TEAM), PRE_GAME");
            assertGates(new forceitembattle.commands.player.CommandVote(null), "ROUND_RUNNING, setting(RUN)");
            assertGates(new forceitembattle.commands.player.CommandVoteSkip(null, null), "ROUND_RUNNING, setting(RUN)");
        }

        /**
         * These gate a <em>subcommand</em> off {@code args[0]}, which a command-level declaration
         * cannot express, so they use {@code requireOp(Player, Runnable)} at the dispatch site.
         * Two of them have no command-level gate at all; {@code /vote} has both.
         */
        @Test
        void theSubcommandGatedCommands() {
            assertGates(new forceitembattle.commands.player.CommandAchievement(null, null), "");
            assertGates(new forceitembattle.commands.player.CommandStats(null, null), "");
        }

        /** Everything else declares nothing, and that is a statement rather than an omission. */
        @Test
        void theUngatedCommandsDeclareEmpty() {
            assertGates(new forceitembattle.commands.player.CommandBed(), "");
            assertGates(new forceitembattle.commands.player.CommandInfo(null, null, null, null), "");
            assertGates(new forceitembattle.commands.player.CommandInfoWiki(null, null), "");
            assertGates(new forceitembattle.commands.player.CommandResult(null, null, null, null, null, null), "");
            assertGates(new forceitembattle.commands.player.CommandSpectate(null), "");
        }
    }

}
