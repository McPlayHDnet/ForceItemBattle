package forceitembattle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * That MockBukkit runs against this plugin's Paper version, and what it unlocks.
 *
 * <p>Everything the unit suite could not reach came down to one thing: {@code new ItemStack(...)}
 * needs the attribute registry, which only a running server has. {@code HeadlessBoundaryTest}
 * has been pinning that boundary since pass 1, and it is why {@code gui/}, {@code listener/} and
 * {@code commands/} have no tests between them.
 *
 * <p>MockBukkit provides the server. The artifact is named after the exact Paper API version —
 * {@code mockbukkit-v26.2} — which is the thing to check first when this stops resolving after a
 * Paper bump: a matching MockBukkit has to exist for the new version.
 */
class MockBukkitSmokeTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void theServerBoots() {
        assertNotNull(this.server);
        assertNotNull(this.server.getVersion());
    }

    /**
     * The wall falls. This is the single assertion that makes three untested packages reachable:
     * with a server present, an ItemStack can be constructed rather than only mocked.
     */
    @Test
    void anItemStackCanFinallyBeConstructed() {
        ItemStack stack = new ItemStack(Material.STONE, 3);

        assertEquals(Material.STONE, stack.getType());
        assertEquals(3, stack.getAmount());
    }

    /** Item meta too, which is what the custom items and the joker stack are built out of. */
    @Test
    void itemMetaIsReal() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        var meta = stack.getItemMeta();

        assertNotNull(meta);
        meta.setCustomModelData(7);
        stack.setItemMeta(meta);

        assertEquals(7, stack.getItemMeta().getCustomModelData());
    }

    /** A real player with a real inventory — what every listener test needs. */
    @Test
    void aPlayerHasARealInventory() {
        PlayerMock player = this.server.addPlayer("Understudy1");
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND));

        assertEquals(Material.DIAMOND, player.getInventory().getItem(0).getType());
        assertTrue(player.isOnline());
    }
}
