package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import forceitembattle.commands.admin.CommandSkip;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@code /skip}: who may run it, and who it acts on.
 *
 * <p>This command spent an unknown period doing the opposite of what it says. Its op gate was
 * written {@code if (!requireOp(player)) { ...whole body... }} — the one site of thirteen that
 * opened a block instead of returning — so it ran in full for <b>non-ops</b>, after telling them
 * they lacked permission, and did nothing at all for an op.
 *
 * <p>The gate is a declared {@link Precondition} now, so the inversion is unrepresentable. These
 * pin the outcome anyway: the whole point of the command is that a skip reaches the target.
 */
class CommandSkipTest {

    private ServerMock server;
    private RoundPhase roundPhase;
    private Gamemanager gamemanager;
    private CommandSkip command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roundPhase = new RoundPhase();
        this.gamemanager = mock(Gamemanager.class);

        this.command = new CommandSkip(this.gamemanager);
        ((CustomCommand) this.command).setContext(
                new CommandContext(this.roundPhase, null, new Roster()));

        this.roundPhase.moveTo(GameState.MID_GAME);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock op(String name) {
        PlayerMock player = this.server.addPlayer(name);
        player.setOp(true);
        return player;
    }

    /** The inversion, pinned from the other side: a non-op must not reach the body. */
    @Test
    void aNonOpSkipsNobody() {
        PlayerMock caller = this.server.addPlayer("Understudy1");
        caller.setOp(false);
        PlayerMock target = this.server.addPlayer("Understudy2");

        this.command.onCommand(caller, null, "skip", new String[]{target.getName()});

        assertTrue(caller.nextMessage().contains("permission"));
        verify(this.gamemanager, never()).forceSkipItem(any());
    }

    @Test
    void anOpSkipsTheNamedPlayer() {
        PlayerMock caller = op("Understudy1");
        PlayerMock target = this.server.addPlayer("Understudy2");

        this.command.onCommand(caller, null, "skip", new String[]{target.getName()});

        verify(this.gamemanager).forceSkipItem(target);
        assertTrue(caller.nextMessage().contains("Skipped this item for Understudy2"));
    }

    @Test
    void anOfflineTargetIsReportedAndNothingIsSkipped() {
        PlayerMock caller = op("Understudy1");

        this.command.onCommand(caller, null, "skip", new String[]{"Nobody"});

        assertTrue(caller.nextMessage().contains("not online"));
        verify(this.gamemanager, never()).forceSkipItem(any());
    }

    @Test
    void theUsageIsShownWithoutATarget() {
        PlayerMock caller = op("Understudy1");

        this.command.onCommand(caller, null, "skip", new String[0]);

        assertTrue(caller.nextMessage().contains("Usage: /skip"));
        verify(this.gamemanager, never()).forceSkipItem(any());
    }

    /** Declared as ROUND_RUNNING, so a skip outside a round never reaches the body. */
    @Test
    void nothingIsSkippedOutsideARound() {
        this.roundPhase.moveTo(GameState.PRE_GAME);
        PlayerMock caller = op("Understudy1");
        PlayerMock target = this.server.addPlayer("Understudy2");

        this.command.onCommand(caller, null, "skip", new String[]{target.getName()});

        assertTrue(caller.nextMessage().contains("game is not running"));
        verify(this.gamemanager, never()).forceSkipItem(any());
    }
}
