package forceitembattle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Where the headless test boundary sits, established by experiment.
 *
 * Most of the plugin turns out to be reachable without booting a server: the {@link Material} enum
 * initialises, Bukkit interfaces mock (given {@code -Dnet.bytebuddy.experimental=true}, set on the
 * test task — Byte Buddy does not yet recognise Java 25), and the whole plugin/manager graph mocks
 * deeply enough that {@code ItemDifficultiesManager.enable()} registers all ~1,367 items.
 *
 * <p>{@link org.bukkit.inventory.ItemStack} is the wall. Its static init reaches for the attribute
 * registry, which only exists on a running server, so everything that builds one — every GUI,
 * ItemBuilder, the joker inventory handling — is out of reach here and has to be checked in a real
 * game or under a harness like MockBukkit.
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
     * A second wall, found the hard way while trying to test {@code Rarity}.
     *
     * <p>{@link org.bukkit.Sound} is {@code Registry}-backed on modern Paper, and the registry only
     * exists on a running server. So any enum holding a {@code Sound} constant cannot be
     * class-initialised here — the failure is a {@code NoClassDefFoundError} on
     * {@code org.bukkit.Registry}, one frame removed from the thing you were actually testing,
     * which makes it easy to misread as a build problem.
     *
     * <p>Concretely: {@code model/Rarity} pairs each rarity's stat mapping with the sound it plays,
     * so the read/write symmetry of that table is not assertable headless even though the mapping
     * itself is now pure. Splitting the sound out to reach it would be letting the test harness
     * design the module.
     */
    @Test
    void aRegistryBackedEnumConstantStillNeedsARunningServer() {
        assertThrows(Throwable.class, () -> org.bukkit.Sound.BLOCK_BEACON_ACTIVATE.getKey());
    }

    /**
     * The wall is narrower than "ItemStack": it is the <em>constructor</em>, which reaches for the
     * attribute registry. Mocking one is fine, so code that only ever calls {@code getType()} on a
     * stack handed to it is reachable here — which is what lets the achievement handlers be tested
     * against a {@code FoundItemEvent}, since that event carries a stack rather than a Material.
     *
     * <p>Worth keeping straight: this does not make the GUI layer testable. Those classes
     * <em>build</em> stacks, and no amount of mocking helps with that.
     */
    @Test
    void anItemStackCanStillBeMocked() {
        org.bukkit.inventory.ItemStack stack =
                org.mockito.Mockito.mock(org.bukkit.inventory.ItemStack.class);
        org.mockito.Mockito.when(stack.getType()).thenReturn(Material.STONE);

        assertNotNull(stack.getType());
    }
}
