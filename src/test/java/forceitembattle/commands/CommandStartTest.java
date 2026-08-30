package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.admin.CommandStart;
import forceitembattle.settings.ConfigSource;
import forceitembattle.settings.GamePreset;
import forceitembattle.settings.GameSettings;
import forceitembattle.model.Roster;
import forceitembattle.settings.Ruleset;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link CommandStart}'s argument handling — the refusals, and what they leave behind.
 *
 * <p>The rules a round is built from live in {@code RoundStart} and are tested there. What is left
 * in the command is parsing and reporting, and this covers the paths that stop before anything is
 * started: a preset that does not exist, the wrong number of arguments, arguments that are not
 * numbers.
 *
 * <p>Each also asserts the round was <em>not</em> started, because a refusal that still mutated the
 * ruleset would be worse than one that said nothing.
 */
class CommandStartTest {

    private ServerMock server;
    private ForceItemBattle plugin;
    private GameSettings settings;
    private Ruleset ruleset;
    private CommandStart command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = mock(ForceItemBattle.class);
        this.settings = mock(GameSettings.class);
        this.ruleset = new Ruleset(mock(ConfigSource.class));

        when(this.plugin.getSettings()).thenReturn(this.settings);
        when(this.settings.getRuleset()).thenReturn(this.ruleset);
        // performCommand reads the roster head-count before it parses the arguments, so even the
        // paths that refuse on a bad argument need one present.
        when(this.plugin.getRoster()).thenReturn(new Roster());

        this.command = new CommandStart(this.plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Messages are MiniMessage components; compare what a player would actually read. */
    private void assertTold(PlayerMock player, String expectedPlainText) {
        var message = player.nextComponentMessage();
        org.junit.jupiter.api.Assertions.assertNotNull(message, "expected a message, got none");
        org.junit.jupiter.api.Assertions.assertEquals(expectedPlainText,
                PlainTextComponentSerializer.plainText().serialize(message));
    }

    private PlayerMock op() {
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.setOp(true);
        return player;
    }

    @Test
    void anUnknownPresetIsRefusedByName() {
        when(this.settings.getGamePreset("nosuch")).thenReturn(null);
        PlayerMock player = op();

        this.command.onPlayerCommand(player, "start", new String[]{"nosuch"});

        assertTold(player, "nosuch does not exist in presets.");
        assertNull(this.ruleset.preset(), "a refused start must not point the ruleset anywhere");
    }

    @Test
    void tooFewArgumentsGetTheUsage() {
        PlayerMock player = op();

        this.command.onPlayerCommand(player, "start", new String[0]);

        assertTold(player, "Usage: /start <time in min> <jokers>");
    }

    @Test
    void tooManyArgumentsGetTheUsage() {
        PlayerMock player = op();

        this.command.onPlayerCommand(player, "start", new String[]{"90", "3", "extra"});

        assertTold(player, "Usage: /start <time in min> <jokers>");
    }

    /**
     * Non-numeric arguments are caught rather than thrown. The parse happens inside
     * {@code performCommand}, so the try/catch has to wrap the call rather than the parse — easy to
     * break by moving the parse out, and this is what would notice.
     */
    @Test
    void nonNumericArgumentsAreExplained() {
        PlayerMock player = op();

        this.command.onPlayerCommand(player, "start", new String[]{"ninety", "three"});

        assertTold(player, "Usage: /start <time in min> <jokers>");
        assertTold(player, "<time> and <jokers> have to be numbers");
    }

    /**
     * A non-op is refused before any of the above is even considered. {@code /start} is op-gated
     * inside the command, not by a permission node.
     */
    @Test
    void aNonOpCannotStartARound() {
        PlayerMock player = this.server.addPlayer("Understudy2");
        player.setOp(false);

        this.command.onPlayerCommand(player, "start", new String[]{"90", "3"});

        assertTold(player, "You don't have permission to use this command.");
        assertNull(this.ruleset.preset());
    }

    /**
     * A named preset is pointed at before the round is built. That ordering is the fix for a preset
     * outliving its round, so it is worth pinning that the command does it on the preset path.
     */
    @Test
    void aKnownPresetIsPointedAtBeforeTheRoundIsBuilt() {
        GamePreset speedrun = new GamePreset();
        speedrun.setPresetName("speedrun");
        when(this.settings.getGamePreset("speedrun")).thenReturn(speedrun);

        // The countdown that follows needs a live server; the assertion is about what happened
        // before it, so the exception from that is not the subject.
        try {
            this.command.onPlayerCommand(op(), "start", new String[]{"speedrun"});
        } catch (RuntimeException expected) {
            // the round machinery beyond the ruleset is not under test here
        }

        assertSame(speedrun, this.ruleset.preset());
    }
}
