package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import forceitembattle.ForceItemBattle;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link CustomCommand}: the three rules every one of the 35 commands inherits.
 *
 * <p>Who may run a command, and from where, is decided here and nowhere else — op-gating is
 * {@code requireOp(player)} inside a command rather than a permission node, and console senders are
 * refused unless a command opts in. A regression here would not break one command; it would break
 * or unlock all of them at once.
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
        private boolean opWasRequired;

        ProbeCommand() {
            super(mock(ForceItemBattle.class), "probe");
        }

        @Override
        public void onPlayerCommand(Player player, String label, String[] args) {
            this.opWasRequired = requireOp(player);
            this.ran.add("player");
        }
    }

    /** A probe that also accepts the console, the way {@code /start} does. */
    private static final class ConsoleFriendlyCommand extends CustomCommand {

        private final List<String> ran = new ArrayList<>();

        ConsoleFriendlyCommand() {
            super(mock(ForceItemBattle.class), "consoleprobe");
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

    @Test
    void aPlayerIsRoutedToThePlayerPath() {
        ProbeCommand command = new ProbeCommand();
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.setOp(true);

        command.onCommand(player, null, "probe", new String[0]);

        assertEquals(List.of("player"), command.ran);
    }

    /**
     * An op passes the gate. This is the whole authorisation model — there is no permission node
     * behind it, so this assertion is the model.
     */
    @Test
    void anOpPassesTheGate() {
        ProbeCommand command = new ProbeCommand();
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.setOp(true);

        command.onCommand(player, null, "probe", new String[0]);

        assertTrue(command.opWasRequired);
    }

    @Test
    void aNonOpIsRefusedAndTold() {
        ProbeCommand command = new ProbeCommand();
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.setOp(false);

        command.onCommand(player, null, "probe", new String[0]);

        assertFalse(command.opWasRequired, "requireOp must report the refusal to the caller");
        player.assertSaid("§cYou don't have permission to use this command.");
    }

    /**
     * The console is refused by default. It matters that this is the default rather than the rule:
     * {@code /start} overrides it deliberately so a server owner — or the round-test harness over
     * RCON — can start a round without a player online.
     */
    @Test
    void theConsoleIsRefusedByDefault() {
        ProbeCommand command = new ProbeCommand();
        ConsoleCommandSenderMock console = this.server.getConsoleSender();

        command.onCommand(console, null, "probe", new String[0]);

        assertTrue(command.ran.isEmpty(), "the player path must not run for the console");
        console.assertSaid("This command can only be executed by a player");
    }

    @Test
    void aCommandMayOptIntoTheConsole() {
        ConsoleFriendlyCommand command = new ConsoleFriendlyCommand();

        command.onCommand(this.server.getConsoleSender(), null, "consoleprobe", new String[0]);

        assertEquals(List.of("console"), command.ran);
    }

    @Test
    void onCommandAlwaysReportsHandled() {
        ProbeCommand command = new ProbeCommand();
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.setOp(false);

        assertTrue(command.onCommand(player, null, "probe", new String[0]),
                "returning false would make Bukkit print the plugin.yml usage on top of ours");
    }
}
