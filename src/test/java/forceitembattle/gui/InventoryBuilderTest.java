package forceitembattle.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link InventoryBuilder}, which all fourteen menus extend and none of them work around.
 *
 * <p>Two things are pinned here. The <em>holder identity</em>, because {@code GuiListener} routes
 * every click by asking whether the inventory's holder is an {@code InventoryBuilder} — get that
 * wrong and every menu in the plugin stops responding. And the <em>per-slot dispatch</em>, which is
 * how a menu says what a button does.
 */
class InventoryBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private InventoryClickEvent clickOn(InventoryBuilder menu, PlayerMock player, int slot) {
        InventoryView view = player.openInventory(menu.getInventory());
        return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, slot,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    /** The property {@code GuiListener} dispatches on. */
    @Test
    void theInventoryHoldsItsBuilder() {
        InventoryBuilder menu = new InventoryBuilder(27);

        InventoryHolder holder = menu.getInventory().getHolder();

        assertInstanceOf(InventoryBuilder.class, holder);
        assertSame(menu, holder);
    }

    @Test
    void itemsLandInTheSlotTheyWereGiven() {
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.setItem(13, new ItemStack(Material.DIAMOND));

        assertEquals(Material.DIAMOND, menu.getInventory().getItem(13).getType());
    }

    @Test
    void addItemUsesTheFirstEmptySlot() {
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.addItem(new ItemStack(Material.STONE));
        menu.addItem(new ItemStack(Material.DIRT));

        assertEquals(Material.STONE, menu.getInventory().getItem(0).getType());
        assertEquals(Material.DIRT, menu.getInventory().getItem(1).getType());
    }

    @Test
    void aClickRunsThatSlotsHandler() {
        AtomicInteger clicks = new AtomicInteger();
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.setItem(4, new ItemStack(Material.LIME_DYE), event -> clicks.incrementAndGet());

        menu.handleClick(clickOn(menu, server.addPlayer("Understudy1"), 4));

        assertEquals(1, clicks.get());
    }

    /** A slot with no handler is inert — menus leave most of their slots decorative. */
    @Test
    void aClickOnASlotWithNoHandlerDoesNothing() {
        AtomicInteger clicks = new AtomicInteger();
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.setItem(4, new ItemStack(Material.LIME_DYE), event -> clicks.incrementAndGet());

        menu.handleClick(clickOn(menu, server.addPlayer("Understudy1"), 9));

        assertEquals(0, clicks.get(), "only slot 4 has a handler");
    }

    @Test
    void handlersAreKeyedPerSlotNotShared() {
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.setItem(0, new ItemStack(Material.STONE), event -> first.incrementAndGet());
        menu.setItem(1, new ItemStack(Material.DIRT), event -> second.incrementAndGet());

        PlayerMock player = server.addPlayer("Understudy1");
        menu.handleClick(clickOn(menu, player, 1));

        assertEquals(0, first.get());
        assertEquals(1, second.get());
    }

    @Test
    void aRangeOfSlotsCanShareOneItemAndOneHandler() {
        AtomicInteger clicks = new AtomicInteger();
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.setItems(0, 3, new ItemStack(Material.GRAY_STAINED_GLASS_PANE),
                event -> clicks.incrementAndGet());

        PlayerMock player = server.addPlayer("Understudy1");
        for (int slot = 0; slot <= 3; slot++) {
            assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                    menu.getInventory().getItem(slot).getType());
            menu.handleClick(clickOn(menu, player, slot));
        }

        assertEquals(4, clicks.get());
    }

    @Test
    void removingAnItemClearsTheSlot() {
        InventoryBuilder menu = new InventoryBuilder(27);
        menu.setItem(5, new ItemStack(Material.DIAMOND));
        menu.removeItem(5);

        assertTrue(menu.getInventory().getItem(5) == null
                || menu.getInventory().getItem(5).getType() == Material.AIR);
    }

    @Test
    void aPlayerCanActuallyOpenIt() {
        InventoryBuilder menu = new InventoryBuilder(27);
        PlayerMock player = server.addPlayer("Understudy1");

        player.openInventory(menu.getInventory());

        assertFalse(player.getOpenInventory().getType() == InventoryType.CRAFTING,
                "the menu should be open, not the default crafting view");
        assertSame(menu, player.getOpenInventory().getTopInventory().getHolder());
    }
}
