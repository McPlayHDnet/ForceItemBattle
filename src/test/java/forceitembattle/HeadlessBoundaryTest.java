package forceitembattle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Where the headless test boundary sits, established by experiment.
 *
 * <p>Most of the plugin is reachable without booting a server: the {@link Material} enum initialises,
 * Bukkit interfaces mock (given {@code -Dnet.bytebuddy.experimental=true}, set on the test task —
 * Byte Buddy does not yet recognise Java 25), and the plugin/manager graph mocks deeply enough that
 * {@code ItemDifficultiesManager.enable()} registers all ~1,367 items.
 *
 * <p>{@link org.bukkit.inventory.ItemStack} is the wall: its static init reaches for the attribute
 * registry, which only exists on a running server, so everything that builds one is out of reach here.
 */
class HeadlessBoundaryTest {

    @Test
    void materialEnumInitialisesHeadless() {
        assertNotNull(Material.STONE);
        assertTrue(Material.values().length > 100);
    }

    /**
     * Documents the blocker rather than a behaviour. If this ever stops throwing, the GUI layer
     * became testable — delete this test and the note above with it.
     */
    @Test
    void itemStackStillNeedsARunningServer() {
        assertThrows(Throwable.class, () -> new org.bukkit.inventory.ItemStack(Material.STONE));
    }

    /**
     * {@link org.bukkit.Sound} is {@code Registry}-backed and only resolves because MockBukkit puts
     * the plain Paper API on the test classpath. Pinned because if that ever leaves again, the
     * {@code NoClassDefFoundError} surfaces a frame away from whatever is actually under test.
     */
    @Test
    void aRegistryBackedEnumConstantIsNowReachable() {
        assertNotNull(org.bukkit.Sound.BLOCK_BEACON_ACTIVATE);
    }

    /**
     * The wall is narrower than "ItemStack": it is the <em>constructor</em>. Mocking one is fine, so
     * code that only calls {@code getType()} on a stack handed to it is reachable here. This does not
     * make the GUI layer testable — those classes <em>build</em> stacks.
     */
    @Test
    void anItemStackCanStillBeMocked() {
        org.bukkit.inventory.ItemStack stack =
                org.mockito.Mockito.mock(org.bukkit.inventory.ItemStack.class);
        org.mockito.Mockito.when(stack.getType()).thenReturn(Material.STONE);

        assertNotNull(stack.getType());
    }
}
