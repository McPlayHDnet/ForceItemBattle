package forceitembattle.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.GameState;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link PreGameLockListener}: nothing moves unless the round is running.
 *
 * <p>Four handlers, one rule, and it is the other half of the pause story. Candidate 7 found that
 * six protection gates switched <em>off</em> during a pause while the world kept ticking; this is
 * why that was hard to exploit from inside — the players themselves are frozen out of every
 * inventory action for the whole pause.
 *
 * <p>The rule is deliberately {@code roundRunning()} and not {@code roundInProgress()}: a paused
 * round must stay locked, so this is one of the 41 sites where excluding the pause is the point.
 */
class PreGameLockListenerTest extends ListenerTestBase {

    private PreGameLockListener listener;

    @BeforeEach
    void setUpListener() {
        this.listener = new PreGameLockListener(this.roundPhase);
    }

    private InventoryClickEvent click(PlayerMock player) {
        InventoryView view = player.openInventory(
                server.createInventory(null, InventoryType.CHEST));
        return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    @ParameterizedTest
    @EnumSource(value = GameState.class, names = {"PRE_GAME", "STARTING", "PAUSED_GAME", "END_GAME"})
    void inventoryClicksAreLockedOutsideARunningRound(GameState state) {
        phase(state);

        InventoryClickEvent event = click(player("Understudy1"));
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled(), state + " must not allow inventory clicks");
    }

    @Test
    void inventoryClicksAreAllowedWhileTheRoundRuns() {
        phase(GameState.MID_GAME);

        InventoryClickEvent event = click(player("Understudy1"));
        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    /**
     * The pause specifically. Stated on its own because it is the case the two predicates disagree
     * about, and the one a future edit is most likely to get wrong by reaching for
     * {@code roundInProgress()}.
     */
    @Test
    void aPausedRoundIsLockedEvenThoughItHasStarted() {
        phase(GameState.PAUSED_GAME);

        InventoryClickEvent event = click(player("Understudy1"));
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }
}
