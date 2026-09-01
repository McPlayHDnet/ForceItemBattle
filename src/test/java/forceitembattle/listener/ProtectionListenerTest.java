package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.ProtectionManager;
import forceitembattle.model.GameState;
import forceitembattle.model.ProtectionVerdict;
import forceitembattle.model.Roster;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link ProtectionListener}, and specifically the pause.
 *
 * <p>Six of the nine protection gates {@code return} rather than refusing, so gating them on
 * {@code roundRunning()} switches protection off while the Minecraft world keeps ticking. They must
 * ask {@code roundInProgress()}.
 */
class ProtectionListenerTest extends ListenerTestBase {

    private ProtectionManager protection;
    private ProtectionListener listener;

    @BeforeEach
    void setUpListener() {
        this.protection = mock(ProtectionManager.class);
        ForceItemBattle plugin = mock(ForceItemBattle.class);
        this.listener = new ProtectionListener(plugin, new Roster(), this.roundPhase, this.protection);
    }

    private BlockBreakEvent breakAt(PlayerMock player, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.STONE);
        return new BlockBreakEvent(block, player);
    }

    /** Outside a round nothing may be broken at all — this gate cancels rather than skipping. */
    @ParameterizedTest
    @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING", "END_GAME"})
    void breakingIsRefusedOutrightOutsideARound(GameState state) {
        phase(state);

        BlockBreakEvent event = breakAt(player("Understudy1"), 0, 64, 0);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
        verify(protection, never()).mayBreak(any(), any(), any());
    }

    @Test
    void aPermittedBreakIsAllowedWhileTheRoundRuns() {
        phase(GameState.MID_GAME);
        when(protection.mayBreak(any(), any(), any())).thenReturn(ProtectionVerdict.ALLOWED);

        BlockBreakEvent event = breakAt(player("Understudy1"), 0, 64, 0);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void aRefusedBreakIsCancelled() {
        phase(GameState.MID_GAME);
        when(protection.mayBreak(any(), any(), any())).thenReturn(ProtectionVerdict.NEAR_BED);

        BlockBreakEvent event = breakAt(player("Understudy1"), 0, 64, 0);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
    }

    /**
     * A pause stops this plugin's clock, not the world's, so protection has to keep applying. If this
     * gate ever reverts to {@code roundRunning()}, the check is skipped for the whole pause.
     */
    @Test
    void protectionStillApplyDuringAPause() {
        phase(GameState.PAUSED_GAME);
        when(protection.mayBreak(any(), any(), any())).thenReturn(ProtectionVerdict.NEAR_BED);

        BlockBreakEvent event = breakAt(player("Understudy1"), 0, 64, 0);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "a protected block stays protected while paused");
        verify(protection).mayBreak(any(), any(), any());
    }

    /** And the paused round is still a round, so an unprotected block is not refused outright. */
    @Test
    void aPermittedBreakDuringAPauseIsDecidedByProtectionNotByThePhase() {
        phase(GameState.PAUSED_GAME);
        when(protection.mayBreak(any(), any(), any())).thenReturn(ProtectionVerdict.ALLOWED);

        BlockBreakEvent event = breakAt(player("Understudy1"), 0, 64, 0);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
    }
}
