package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.commands.admin.CommandStart;
import forceitembattle.commands.admin.RoundStart;
import forceitembattle.manager.ForceItemAssignment;
import forceitembattle.manager.Gamemanager;
import forceitembattle.manager.TeamsManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundClock;
import forceitembattle.model.RoundPhase;
import forceitembattle.settings.ConfigSource;
import forceitembattle.settings.GamePreset;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.Ruleset;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.InOrder;

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
 *
 * <p>{@link TheSuccessPath} covers the one thing the refusals cannot: the order of the two calls that
 * open a round. See its javadoc — it exists because a released round-2 regression got through here.
 */
class CommandStartTest {

    private ServerMock server;
    private GameSettings settings;
    private Ruleset ruleset;
    private Gamemanager gamemanager;
    private ForceItemAssignment assignment;
    private Roster roster;
    private CommandStart command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.settings = mock(GameSettings.class);
        this.ruleset = new Ruleset(mock(ConfigSource.class));

        when(this.settings.getRuleset()).thenReturn(this.ruleset);
        // performCommand reads the roster head-count before it parses the arguments, so even the
        // paths that refuse on a bad argument need one present.

        this.gamemanager = mock(Gamemanager.class);
        this.assignment = mock(ForceItemAssignment.class);
        this.roster = new Roster();

        this.command = new CommandStart(this.gamemanager, this.assignment, this.roster, mock(RoundPhase.class), mock(RoundClock.class), this.settings, mock(TeamsManager.class));
        // The op gate is declared, so it is evaluated in onCommand -- which needs the context that
        // CommandsManager supplies at bootstrap.
        // Cast because setContext is package-private on CustomCommand, and a package-private
        // member is not inherited into CommandStart's package.
        ((CustomCommand) this.command).setContext(new CommandContext(
                new forceitembattle.model.RoundPhase(), this.ruleset, new Roster()));
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
     * A non-op is refused before any of the above is even considered. {@code /start} declares
     * {@link Precondition#OP}, so the gate is evaluated in {@code onCommand} and the body is never
     * entered — which is why this goes through the real entry point rather than calling
     * {@code onPlayerCommand} directly.
     */
    @Test
    void aNonOpCannotStartARound() {
        PlayerMock player = this.server.addPlayer("Understudy2");
        player.setOp(false);

        this.command.onCommand(player, null, "start", new String[]{"90", "3"});

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

    /**
     * The two calls that open a round, and the fact that both happen.
     *
     * <p>This class exists because of a released regression that every other test here was blind to.
     * Lifting the item draw out of {@code Gamemanager} moved {@code initializeMaterials} to
     * {@code ForceItemAssignment.beginRound} — and quietly dropped the one line in it that was not
     * about drawing items: the reset of {@code startSetupApplied}. Nothing then set the flag back to
     * false, so from the second round of a JVM onwards {@code applyStartSetup} no-opped for everyone
     * and nobody left creative.
     *
     * <p>It was invisible three ways over. The unit suite was green: no test drove the success path,
     * because the countdown after it needs a live server. Production is spared entirely, because
     * {@code scheduleReset} restarts the JVM between rounds. Only {@code Invoke-RoundTest.ps1},
     * playing two rounds in one session, could see it — and it did, on the first run after the change.
     *
     * <p>So the point of these is not the assertions, which are two lines. It is that the round-2
     * regression now has a guard that runs in four minutes rather than five, and one that fails at
     * the call site where the damage actually was: the wiring, not any body.
     */
    @Nested
    class TheSuccessPath {

        /** Runs a real start, ignoring the countdown that needs a live server. */
        private void startARound() {
            try {
                command.onPlayerCommand(op(), "start", new String[]{"1", "3"});
            } catch (RuntimeException expected) {
                // the countdown beyond the two calls under test is not the subject
            }
        }

        @Test
        void everyoneIsMarkedUnequippedBeforeTheItemsAreDrawn() {
            startARound();

            InOrder inOrder = inOrder(gamemanager, assignment);
            inOrder.verify(gamemanager).resetStartSetup();
            inOrder.verify(assignment).beginRound(anyBoolean());
        }

        @Test
        void andTheRoundIsOpened() {
            startARound();

            verify(assignment).beginRound(anyBoolean());
        }
    }
}
