package forceitembattle.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.manager.Gamemanager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameItems;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The two items the game gives out, and the questions asked about them afterwards.
 *
 * <p>These are the {@code Gamemanager} statics that build and recognise the joker stack and the
 * backpack. They matter more than they look: on death, {@code PlayerLifecycleListener} filters the
 * drops with {@code removeIf(GameItems::isJoker)} and {@code removeIf(GameItems::isBackpack)},
 * so a recogniser that stops matching what the builder produces means players drop — and lose —
 * their jokers and backpack when they die.
 *
 * <p>Neither half could be tested before: one constructs an {@code ItemStack}, the other reads a
 * persistent data container off one.
 */
class GameItemsTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ForceItemPlayer participant(String name) {
        PlayerMock player = this.server.addPlayer(name);
        return new ForceItemPlayer(player, Material.DIRT, 0, 0);
    }

    // --- jokers ---------------------------------------------------------------------------

    @Test
    void aJokerStackCarriesTheRequestedAmount() {
        assertEquals(3, GameItems.jokers(3).getAmount());
    }

    /** The round trip. This is the pair the death handler depends on agreeing. */
    @Test
    void aBuiltJokerIsRecognisedAsOne() {
        assertTrue(GameItems.isJoker(GameItems.jokers(1)));
    }

    @Test
    void anOrdinaryItemIsNotAJoker() {
        assertFalse(GameItems.isJoker(new ItemStack(Material.DIAMOND)));
    }

    // --- backpacks ------------------------------------------------------------------------

    @Test
    void aBuiltBackpackIsRecognisedAsOne() {
        ItemStack backpack = GameItems.backpack(participant("Understudy1"), false);

        assertTrue(GameItems.isBackpack(backpack));
    }

    /**
     * The distinction the persistent data container exists for. A player can obtain an ordinary
     * bundle — it is a real item and can be a force item — and it must not be mistaken for the
     * backpack, or it would survive death while the real one is indistinguishable from it.
     */
    @Test
    void aPlainBundleIsNotABackpack() {
        assertFalse(GameItems.isBackpack(new ItemStack(Material.BUNDLE)));
    }

    @Test
    void anOrdinaryItemIsNotABackpack() {
        assertFalse(GameItems.isBackpack(new ItemStack(Material.DIAMOND)));
    }

    // --- what death does with them -----------------------------------------------------------

    /**
     * The behaviour those two recognisers are for, exercised the way the death handler does it:
     * jokers and the backpack are removed from the drops, everything else falls.
     */
    @Test
    void deathDropsKeepLootAndRemoveTheGameItems() {
        List<ItemStack> drops = new ArrayList<>(List.of(
                new ItemStack(Material.DIAMOND),
                GameItems.jokers(2),
                GameItems.backpack(participant("Understudy1"), false),
                new ItemStack(Material.COBBLESTONE)));

        drops.removeIf(GameItems::isJoker);
        drops.removeIf(GameItems::isBackpack);

        assertEquals(2, drops.size());
        assertTrue(drops.stream().anyMatch(s -> s.getType() == Material.DIAMOND));
        assertTrue(drops.stream().anyMatch(s -> s.getType() == Material.COBBLESTONE));
    }
}
