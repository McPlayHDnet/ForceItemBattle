package forceitembattle.commands;

import static forceitembattle.commands.CommandTestSupport.assertSaid;
import static forceitembattle.commands.CommandTestSupport.screenOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.commands.player.CommandSpectate;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
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
 * <p>The other half is the phase gate, which is the only thing standing between a player and
 * creative mode mid-round. It used to ask the clock instead: {@code getTimeLeft() > 0} is
 * {@code !isEndGame()} written in terms of a counter, and the counter is loaded from config before
 * a round has been played and frozen above zero during a pause.
 */
class CommandSpectateTest {

    private ServerMock server;
    private RoundPhase phase;
    private CommandSpectate command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();

        this.phase = new RoundPhase();

        this.command = new CommandSpectate(this.phase);
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
        this.phase.moveTo(GameState.END_GAME);
    }

    private void inState(GameState state) {
        this.phase.moveTo(state);
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

    /** The gate: nobody escapes into creative while the round is still on. */
    @Test
    void midRoundItIsRefused() {
        PlayerMock player = join("Understudy1", GameMode.SPECTATOR);
        inState(GameState.MID_GAME);

        run(player);

        assertSaid(player, "after the game end");
        assertEquals(GameMode.SPECTATOR, player.getGameMode());
    }

    /**
     * A pause is not the end of a round. The clock stops there and stays above zero, so the old
     * counter gate agreed by accident; the phase says so directly.
     */
    @Test
    void aPauseIsNotTheEnd() {
        PlayerMock player = join("Understudy1", GameMode.SPECTATOR);
        inState(GameState.PAUSED_GAME);

        run(player);

        assertEquals(GameMode.SPECTATOR, player.getGameMode());
    }

    /**
     * And neither is the lobby. Before any round has been played the clock holds the configured
     * duration, which is where the counter and the phase actually disagreed.
     */
    @Test
    void beforeAnyRoundItIsRefused() {
        PlayerMock player = join("Understudy1", GameMode.SPECTATOR);
        inState(GameState.PRE_GAME);

        run(player);

        assertSaid(player, "after the game end");
        assertEquals(GameMode.SPECTATOR, player.getGameMode());
    }
}
