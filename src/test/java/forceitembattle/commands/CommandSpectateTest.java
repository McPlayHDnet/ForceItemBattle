package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.commands.player.CommandSpectate;
import forceitembattle.manager.TimerManager;
import org.bukkit.GameMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /spectate}: leaving spectator mode once the round is over.
 *
 * <p>Its own description says "Toggle gamemode spectator", and it does not toggle. There is one
 * branch and it only runs when the player <em>is</em> already spectating, so the command can take
 * you out of spectator but never put you in; run by anyone else it silently does nothing at all.
 * That is recorded here rather than corrected, because which of the two the server actually wants
 * is not a question the tests get to answer — the round-end flow puts players into spectator
 * itself, so a one-way exit may well be the intent and the description the thing that is wrong.
 * If a real toggle is added, {@link #someoneNotSpectatingIsSilentlyIgnored} is the test that
 * changes.
 *
 * <p>The other half is the timer gate, which is the only thing standing between a player and
 * creative mode mid-round.
 */
class CommandSpectateTest {

    private ServerMock server;
    private TimerManager timer;
    private CommandSpectate command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        this.timer = mock(TimerManager.class);

        this.command = new CommandSpectate(this.timer);
        ((CustomCommand) this.command).setContext(new CommandContext(null, null, null));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock join(String name, GameMode mode) {
        PlayerMock player = this.server.addPlayer(name);
        player.setGameMode(mode);
        return player;
    }

    private void roundIsOver() {
        when(this.timer.getTimeLeft()).thenReturn(0);
    }

    private void secondsLeft(int seconds) {
        when(this.timer.getTimeLeft()).thenReturn(seconds);
    }

    private void run(PlayerMock player) {
        this.command.onCommand(player, null, "spectate", new String[0]);
    }

    @Test
    void afterTheRoundASpectatorIsPutBackIntoCreative() {
        PlayerMock player = join("Understudy1", GameMode.SPECTATOR);
        roundIsOver();

        run(player);

        assertEquals(GameMode.CREATIVE, player.getGameMode());
        assertSaid(player, "no longer");
    }

    /**
     * The half the description promises and the command does not do. Recorded, not endorsed: see
     * the class note.
     */
    @Test
    void someoneNotSpectatingIsSilentlyIgnored() {
        PlayerMock player = join("Understudy1", GameMode.SURVIVAL);
        roundIsOver();

        run(player);

        assertEquals(GameMode.SURVIVAL, player.getGameMode(),
                "there is no branch that enters spectator mode");
        assertTrue(screenOf(player).isEmpty(), "and nothing is said about it either");
    }

    /** The gate: nobody escapes into creative while the clock is still running. */
    @Test
    void midRoundItIsRefused() {
        PlayerMock player = join("Understudy1", GameMode.SPECTATOR);
        secondsLeft(300);

        run(player);

        assertSaid(player, "after the game end");
        assertEquals(GameMode.SPECTATOR, player.getGameMode());
    }

    /** One second left is still mid-round. */
    @Test
    void theGateOpensOnlyAtZero() {
        PlayerMock player = join("Understudy1", GameMode.SPECTATOR);
        secondsLeft(1);

        run(player);

        assertEquals(GameMode.SPECTATOR, player.getGameMode());
    }
}
