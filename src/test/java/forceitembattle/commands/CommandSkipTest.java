package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import forceitembattle.commands.admin.CommandSkip;
import forceitembattle.manager.ForceItemAssignment;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSettings;
import org.bukkit.Material;
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
    private ForceItemAssignment assignment;
    private Roster roster;
    private CommandSkip command;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.roundPhase = new RoundPhase();
        this.assignment = mock(ForceItemAssignment.class);
        this.roster = new Roster();

        this.command = new CommandSkip(this.assignment, this.roster, mock(GameSettings.class));
        ((CustomCommand) this.command).setContext(
                new CommandContext(this.roundPhase, null, this.roster));

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
        verify(this.assignment, never()).skipAll(any(), anyBoolean());
    }

    /** On the roster, which is what {@code /skip} now needs before it will act. */
    private ForceItemPlayer onRoster(PlayerMock player) {
        ForceItemPlayer entry = new ForceItemPlayer(player, Material.DIRT, 0, 0);
        this.roster.add(player.getUniqueId(), entry);
        return entry;
    }

    @Test
    void anOpSkipsTheNamedPlayer() {
        PlayerMock caller = op("Understudy1");
        PlayerMock target = this.server.addPlayer("Understudy2");
        ForceItemPlayer targetEntry = onRoster(target);

        this.command.onCommand(caller, null, "skip", new String[]{target.getName()});

        verify(this.assignment).skipAll(targetEntry, false);
        assertTrue(caller.nextMessage().contains("Skipped this item for Understudy2"));
    }

    /**
     * The target has to be <em>in the round</em>, not merely online. The old code took a
     * {@code Player} and let the assignment side look it up, so this case returned silently and the
     * admin was told the skip had happened. Now it is refused where the admin can see it.
     */
    @Test
    void anOnlineTargetOutsideTheRoundIsRefused() {
        PlayerMock caller = op("Understudy1");
        PlayerMock target = this.server.addPlayer("Understudy2");

        this.command.onCommand(caller, null, "skip", new String[]{target.getName()});

        assertTrue(caller.nextMessage().contains("not in the round"));
        verify(this.assignment, never()).skipAll(any(), anyBoolean());
    }

    @Test
    void anOfflineTargetIsReportedAndNothingIsSkipped() {
        PlayerMock caller = op("Understudy1");

        this.command.onCommand(caller, null, "skip", new String[]{"Nobody"});

        assertTrue(caller.nextMessage().contains("not online"));
        verify(this.assignment, never()).skipAll(any(), anyBoolean());
    }

    @Test
    void theUsageIsShownWithoutATarget() {
        PlayerMock caller = op("Understudy1");

        this.command.onCommand(caller, null, "skip", new String[0]);

        assertTrue(caller.nextMessage().contains("Usage: /skip"));
        verify(this.assignment, never()).skipAll(any(), anyBoolean());
    }

    /** Declared as ROUND_RUNNING, so a skip outside a round never reaches the body. */
    @Test
    void nothingIsSkippedOutsideARound() {
        this.roundPhase.moveTo(GameState.PRE_GAME);
        PlayerMock caller = op("Understudy1");
        PlayerMock target = this.server.addPlayer("Understudy2");

        this.command.onCommand(caller, null, "skip", new String[]{target.getName()});

        assertTrue(caller.nextMessage().contains("game is not running"));
        verify(this.assignment, never()).skipAll(any(), anyBoolean());
    }
}
