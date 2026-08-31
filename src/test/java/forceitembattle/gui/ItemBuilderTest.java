package forceitembattle.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * {@link ItemBuilder}, which builds every item in the plugin — the joker stack, the backpack, the
 * lobby items, every slot of all fourteen menus.
 *
 * <p>It was unreachable for the most basic reason there is: its constructor calls
 * {@code new ItemStack(material)}. {@code HeadlessBoundaryTest} has recorded that as the wall since
 * pass 1, and it is why {@code gui/} had no tests at all.
 */
class ItemBuilderTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void buildsAStackOfTheGivenMaterial() {
        ItemStack stack = new ItemBuilder(Material.DIAMOND).getItemStack();

        assertEquals(Material.DIAMOND, stack.getType());
        assertEquals(1, stack.getAmount());
    }

    @Test
    void setsTheAmount() {
        assertEquals(7, new ItemBuilder(Material.DIAMOND).setAmount(7).getItemStack().getAmount());
    }

    @Test
    void aDisplayNameIsMiniMessage() {
        ItemStack stack = new ItemBuilder(Material.BARRIER)
                .setDisplayName("<dark_gray>» <dark_purple>Joker")
                .getItemStack();

        assertEquals("» Joker", plain(stack.getItemMeta().displayName()));
    }

    /**
     * The rule worth pinning here. Minecraft italicises a custom display name by default, and every
     * menu in this plugin turns that off — so the builder does it once rather than fourteen menus
     * remembering. A name that came out italic would look wrong everywhere at once.
     */
    @Test
    void aDisplayNameIsNotItalic() {
        ItemStack stack = new ItemBuilder(Material.BARRIER)
                .setDisplayName("<yellow>Backpack")
                .getItemStack();

        assertEquals(TextDecoration.State.FALSE,
                stack.getItemMeta().displayName().decoration(TextDecoration.ITALIC));
    }

    @Test
    void loreLinesAreMiniMessageAndAlsoNotItalic() {
        ItemStack stack = new ItemBuilder(Material.PAPER)
                .setLore(List.of("<gray>first", "<gray>second"))
                .getItemStack();

        List<Component> lore = stack.getItemMeta().lore();
        assertNotNull(lore);
        assertEquals(2, lore.size());
        assertEquals("first", plain(lore.get(0)));
        assertEquals(TextDecoration.State.FALSE, lore.get(0).decoration(TextDecoration.ITALIC));
    }

    /** Null is tolerated and changes nothing — menus pass optional text straight through. */
    @Test
    void aNullNameOrLoreIsIgnored() {
        ItemBuilder builder = new ItemBuilder(Material.STONE);

        assertSame(builder, builder.setDisplayName(null));
        assertSame(builder, builder.setLore(null));
        assertEquals(Material.STONE, builder.getItemStack().getType());
    }

    /** The legacy path is still used by a few menus, and translates {@code &} to the section sign. */
    @Test
    void theLegacyNameConvertsAmpersandColourCodes() {
        ItemStack stack = new ItemBuilder(Material.STONE)
                .setDisplayNameLegacy("&cRed &lBold")
                .getItemStack();

        assertEquals("§cRed §lBold", stack.getItemMeta().getDisplayName());
    }

    @Test
    void itemFlagsAreApplied() {
        ItemStack stack = new ItemBuilder(Material.DIAMOND_SWORD)
                .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
                .getItemStack();

        assertTrue(stack.getItemMeta().hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
    }

    @Test
    void everyFlagAtOnceIsWhatTheLobbyItemsUse() {
        ItemStack stack = new ItemBuilder(Material.WRITTEN_BOOK)
                .addItemFlags(ItemFlag.values())
                .getItemStack();

        for (ItemFlag flag : ItemFlag.values()) {
            assertTrue(stack.getItemMeta().hasItemFlag(flag), flag + " should be set");
        }
    }

    /** Wrapping an existing stack keeps it rather than copying, which the menus rely on. */
    @Test
    void anExistingStackIsWrappedNotReplaced() {
        ItemStack original = new ItemStack(Material.EMERALD, 4);
        ItemStack built = new ItemBuilder(original).setDisplayName("<green>Trade").getItemStack();

        assertSame(original, built);
        assertEquals(4, built.getAmount());
    }
}
