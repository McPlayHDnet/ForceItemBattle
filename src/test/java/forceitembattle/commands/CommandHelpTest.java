package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.player.CommandHelp;
import java.util.List;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /help}: the list every other command's {@code setUsage} and {@code setDescription} feed.
 *
 * <p>It reads the registry rather than a hand-written list, which is the right shape and means the
 * only things that can go wrong are the ones tested here: a command with no usage or no
 * description has to render as a plain line rather than the word "null", and {@code /help} has to
 * leave itself out of its own output.
 *
 * <p>The registry is stood up with two probe commands rather than the real thirty-one. What is
 * being tested is how a line is built from a command's metadata, and the real set would only make
 * the assertions depend on wording that changes for unrelated reasons.
 */
class CommandHelpTest {

    private ServerMock server;
    private CommandsManager commands;
    private CommandHelp help;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        this.commands = mock(CommandsManager.class);

        this.help = new CommandHelp(this.commands);
        ((CustomCommand) this.help).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A command that exists only to be listed. */
    private static final class Probe extends CustomCommand {
        private Probe(String name, String usage, String description) {
            super(name);
            if (usage != null) {
                setUsage(usage);
            }
            if (description != null) {
                setDescription(description);
            }
        }

        @Override
        protected List<Precondition> preconditions() {
            return List.of();
        }

        @Override
        public void onPlayerCommand(Player player, String label, String[] args) {
        }
    }

    private void registryHolds(CustomCommand... entries) {
        when(this.commands.getCommands()).thenReturn(List.of(entries));
    }

    private String helpFor(PlayerMock player) {
        this.help.onCommand(player, null, "help", new String[0]);
        return screenOf(player);
    }

    @Test
    void everyRegisteredCommandIsListedWithItsUsageAndDescription() {
        PlayerMock player = server.addPlayer("Understudy1");
        registryHolds(new Probe("skip", "[player]", "Skip your item"));

        String said = helpFor(player);

        assertTrue(said.contains("/skip [player]"), said);
        assertTrue(said.contains("Skip your item"), said);
    }

    /** A command with no usage renders as a bare name, not "/skip null". */
    @Test
    void aCommandWithNoUsageStillRendersCleanly() {
        PlayerMock player = server.addPlayer("Understudy1");
        registryHolds(new Probe("skip", null, "Skip your item"));

        String said = helpFor(player);

        assertTrue(said.contains("/skip "), said);
        assertFalse(said.contains("null"), said);
    }

    @Test
    void aCommandWithNoDescriptionStillRendersCleanly() {
        PlayerMock player = server.addPlayer("Understudy1");
        registryHolds(new Probe("skip", "[player]", null));

        String said = helpFor(player);

        assertTrue(said.contains("/skip [player]"), said);
        assertFalse(said.contains("null"), said);
    }

    /** {@code /help} is in the registry like everything else, and skips itself. */
    @Test
    void helpDoesNotListItself() {
        PlayerMock player = server.addPlayer("Understudy1");
        registryHolds(this.help, new Probe("skip", null, "Skip your item"));

        String said = helpFor(player);

        assertTrue(said.contains("/skip"), said);
        assertFalse(said.contains("/help"), said);
    }

    @Test
    void theListIsHeaded() {
        PlayerMock player = server.addPlayer("Understudy1");
        registryHolds();

        assertTrue(helpFor(player).contains("ForceItemBattle"));
    }

    @Test
    void everyCommandIsListedNotJustTheFirst() {
        PlayerMock player = server.addPlayer("Understudy1");
        registryHolds(
                new Probe("skip", null, "one"),
                new Probe("stats", null, "two"),
                new Probe("top", null, "three"));

        String said = helpFor(player);

        assertTrue(said.contains("/skip"), said);
        assertTrue(said.contains("/stats"), said);
        assertTrue(said.contains("/top"), said);
    }
}
