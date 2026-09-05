package forceitembattle.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * {@link GridPaging}: the arithmetic and the two page heads.
 *
 * <p>Both halves were unreachable before. {@code CollectionDexInventory} and
 * {@code AchievementInventory} each held their own copy of this — the cursor, the start/end/slot
 * arithmetic, and the button wiring — inside a menu that needed a plugin, a collection manager and
 * a player to construct. The greyed-out states in particular were kept in step across two files by
 * eye.
 */
class GridPagingTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A minimal inventory to draw into, with a viewer so the sounds have somewhere to go. */
    private InventoryBuilder inventoryFor(PlayerMock viewer) {
        InventoryBuilder inventory = new InventoryBuilder(9 * 6);
        inventory.open(viewer);
        return inventory;
    }

    /** Clicks the forward head, which is how the cursor moves without a setter. */
    private void turnForward(InventoryBuilder inventory, PlayerMock viewer) {
        inventory.handleClick(new org.bukkit.event.inventory.InventoryClickEvent(
                viewer.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                GridPaging.NEXT_SLOT,
                org.bukkit.event.inventory.ClickType.LEFT,
                org.bukkit.event.inventory.InventoryAction.PICKUP_ALL));
    }

    private static String nameOf(ItemStack stack) {
        assertNotNull(stack, "expected a page head here");
        return PlainTextComponentSerializer.plainText().serialize(stack.getItemMeta().displayName());
    }

    @Nested
    class PageCount {

        @Test
        void noEntriesStillMakeOnePage() {
            assertEquals(1, GridPaging.pageCount(0));
        }

        @Test
        void aFullPageIsStillOnePage() {
            assertEquals(1, GridPaging.pageCount(36));
        }

        @Test
        void oneOverAFullPageIsTwo() {
            assertEquals(2, GridPaging.pageCount(37));
        }

        @Test
        void countsUpInWholePages() {
            assertEquals(1, GridPaging.pageCount(1));
            assertEquals(1, GridPaging.pageCount(35));
            assertEquals(2, GridPaging.pageCount(72));
            assertEquals(3, GridPaging.pageCount(73));
        }
    }

    @Nested
    class SlotMapping {

        private List<Integer> slotsVisited(GridPaging paging, int total) {
            List<Integer> slots = new ArrayList<>();
            paging.forEachOnPage(total, (index, slot) -> slots.add(slot));
            return slots;
        }

        private List<Integer> indexesVisited(GridPaging paging, int total) {
            List<Integer> indexes = new ArrayList<>();
            paging.forEachOnPage(total, (index, slot) -> indexes.add(index));
            return indexes;
        }

        @Test
        void theFirstEntryLandsInSlotNine() {
            GridPaging paging = new GridPaging();

            assertEquals(9, slotsVisited(paging, 10).get(0));
        }

        @Test
        void slotsAreContiguousFromNine() {
            GridPaging paging = new GridPaging();

            List<Integer> slots = slotsVisited(paging, 36);

            assertEquals(36, slots.size());
            assertEquals(9, slots.get(0));
            assertEquals(44, slots.get(35));
        }

        /** The last page is short, and must stop at the total rather than running on to 36. */
        @Test
        void theLastPageIsClampedToWhatIsLeft() {
            PlayerMock viewer = server.addPlayer("Understudy1");
            InventoryBuilder inventory = inventoryFor(viewer);
            GridPaging paging = new GridPaging();

            paging.draw(inventory, 40, () -> { });
            turnForward(inventory, viewer);
            assertEquals(1, paging.currentPage(), "precondition: on the second page");

            assertEquals(List.of(36, 37, 38, 39), indexesVisited(paging, 40));
            assertEquals(List.of(9, 10, 11, 12), slotsVisited(paging, 40));
        }

        @Test
        void anEmptyListVisitsNothing() {
            GridPaging paging = new GridPaging();

            assertTrue(slotsVisited(paging, 0).isEmpty());
        }

        @Test
        void indexesAreTheCallersOwn() {
            GridPaging paging = new GridPaging();

            List<Integer> indexes = indexesVisited(paging, 5);

            assertEquals(List.of(0, 1, 2, 3, 4), indexes);
        }
    }

    @Nested
    class Controls {

        @Test
        void oneFullPageDrawsNoControls() {
            PlayerMock viewer = server.addPlayer("Understudy1");
            InventoryBuilder inventory = inventoryFor(viewer);

            new GridPaging().draw(inventory, 36, () -> { });

            assertNull(inventory.getInventory().getItem(GridPaging.PREVIOUS_SLOT));
            assertNull(inventory.getInventory().getItem(GridPaging.NEXT_SLOT));
        }

        /**
         * Once there is a second page both heads appear together — the unusable one is drawn in its
         * disabled form rather than disappearing, which is the behaviour the two menus kept in step
         * by hand.
         */
        @Test
        void theFirstPageOffersForwardAndAGreyedBack() {
            PlayerMock viewer = server.addPlayer("Understudy1");
            InventoryBuilder inventory = inventoryFor(viewer);

            new GridPaging().draw(inventory, 37, () -> { });

            assertEquals("« Previous page",
                    nameOf(inventory.getInventory().getItem(GridPaging.PREVIOUS_SLOT)));
            assertEquals("» Next page",
                    nameOf(inventory.getInventory().getItem(GridPaging.NEXT_SLOT)));
        }

        @Test
        void turningForwardMovesTheCursorAndRedraws() {
            PlayerMock viewer = server.addPlayer("Understudy1");
            InventoryBuilder inventory = inventoryFor(viewer);
            GridPaging paging = new GridPaging();
            List<String> redraws = new ArrayList<>();

            paging.draw(inventory, 37, () -> redraws.add("redrawn"));
            inventory.handleClick(clickOn(inventory, GridPaging.NEXT_SLOT, viewer));

            assertEquals(1, paging.currentPage());
            assertEquals(List.of("redrawn"), redraws);
        }

        @Test
        void aDeadClickNeitherMovesNorRedraws() {
            PlayerMock viewer = server.addPlayer("Understudy1");
            InventoryBuilder inventory = inventoryFor(viewer);
            GridPaging paging = new GridPaging();
            List<String> redraws = new ArrayList<>();

            paging.draw(inventory, 37, () -> redraws.add("redrawn"));
            inventory.handleClick(clickOn(inventory, GridPaging.PREVIOUS_SLOT, viewer));

            assertEquals(0, paging.currentPage(), "page 0 has nothing before it");
            assertTrue(redraws.isEmpty(), "a refused turn must not redraw");
        }

        private org.bukkit.event.inventory.InventoryClickEvent clickOn(
                InventoryBuilder inventory, int slot, PlayerMock viewer) {
            return new org.bukkit.event.inventory.InventoryClickEvent(
                    viewer.getOpenInventory(),
                    org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                    slot,
                    org.bukkit.event.inventory.ClickType.LEFT,
                    org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        }
    }

    @Nested
    class Reset {

        @Test
        void sendsTheCursorBackToTheFirstPage() {
            PlayerMock viewer = server.addPlayer("Understudy1");
            InventoryBuilder inventory = inventoryFor(viewer);
            GridPaging paging = new GridPaging();

            paging.draw(inventory, 100, () -> { });
            turnForward(inventory, viewer);
            assertEquals(1, paging.currentPage());

            paging.reset();

            assertEquals(0, paging.currentPage());
        }
    }
}
