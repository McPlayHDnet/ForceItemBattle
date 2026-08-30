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
     * The second wall, and where it went.
     *
     * <p>{@link org.bukkit.Sound} is {@code Registry}-backed, so class-initialising anything
     * holding a {@code Sound} constant used to throw {@code NoClassDefFoundError} on
     * {@code org.bukkit.Registry} — one frame removed from whatever was actually under test, which
     * made it easy to misread as a build problem. {@code model/Rarity} was the casualty: its
     * read/write symmetry was expressible but not assertable.
     *
     * <p>Adopting MockBukkit put the plain Paper API on the test classpath, and that moved this
     * wall. {@code RarityTest} exists again as a result. Kept as an assertion rather than deleted
     * because it is the thing that would silently regress if the Paper API ever left the test
     * classpath again — the symptom would reappear far from the cause.
     */
    @Test
    void aRegistryBackedEnumConstantIsNowReachable() {
        assertNotNull(org.bukkit.Sound.BLOCK_BEACON_ACTIVATE);
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
