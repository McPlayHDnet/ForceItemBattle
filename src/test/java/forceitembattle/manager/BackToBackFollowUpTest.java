package forceitembattle.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameContext;
import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Team;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** The back-to-back find is credited a tick after it is detected; what may happen in that tick. */
class BackToBackFollowUpTest {

    private static final GameContext SOLO = new GameContext(false, false, true, false, false);

    private ServerMock server;
    private RoundPhase phase;
    private BackToBackManager backToBack;
    private final List<FoundItemEvent> credited = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        Scheduler.init(MockBukkit.createMockPlugin());

        this.phase = new RoundPhase();
        this.phase.moveTo(GameState.MID_GAME);

        ItemDifficultiesManager items = mock(ItemDifficultiesManager.class);
        when(items.getAvailableItems()).thenReturn(List.of(Material.DIRT, Material.STONE, Material.SAND));
        when(items.getUnicodeFromMaterial(true, Material.DIRT)).thenReturn("");

        this.backToBack = new BackToBackManager(mock(GameSettings.class), this.phase, items,
                mock(BackpackManager.class), mock(FIBServiceClient.class));

        this.server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onFound(FoundItemEvent event) {
                credited.add(event);
            }
        }, MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        Scheduler.reset();
        MockBukkit.unmock();
    }

    /** Hunting dirt, and already holding some: a back-to-back. */
    private ForceItemPlayer holdingTheirItem() {
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.getInventory().addItem(new ItemStack(Material.DIRT));
        return new ForceItemPlayer(player, Material.DIRT, 0, 0);
    }

    @Test
    void theChainIsCreditedOnTheNextTick() {
        ForceItemPlayer finder = holdingTheirItem();

        this.backToBack.handleAfterFind(finder, SOLO);
        this.server.getScheduler().performTicks(1);

        assertEquals(1, this.credited.size());
        assertEquals(Material.DIRT, this.credited.getFirst().getFoundItem().getType());
        assertTrue(this.credited.getFirst().isBackToBack());
    }

    @Test
    void aRoundThatEndsInThatTickCreditsNothing() {
        ForceItemPlayer finder = holdingTheirItem();

        this.backToBack.handleAfterFind(finder, SOLO);
        this.phase.moveTo(GameState.END_GAME);
        this.server.getScheduler().performTicks(1);

        assertTrue(this.credited.isEmpty(), "a find after the round ended would score past the submitted match");
    }

    @Test
    void aPauseInThatTickCreditsNothing() {
        ForceItemPlayer finder = holdingTheirItem();

        this.backToBack.handleAfterFind(finder, SOLO);
        this.phase.moveTo(GameState.PAUSED_GAME);
        this.server.getScheduler().performTicks(1);

        assertTrue(this.credited.isEmpty());
    }

    /** Found by hand first: the chain must not credit whatever item replaced it. */
    @Test
    void anItemThatMovedOnInThatTickIsNotCredited() {
        ForceItemPlayer finder = holdingTheirItem();

        this.backToBack.handleAfterFind(finder, SOLO);
        finder.setCurrentTeam(new Team(1, Material.STONE, 0, 0, finder));
        this.server.getScheduler().performTicks(1);

        assertTrue(this.credited.isEmpty());
    }
}
