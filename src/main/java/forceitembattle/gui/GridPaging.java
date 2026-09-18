package forceitembattle.gui;

import org.bukkit.Sound;

/**
 * Paging for the six-row collection grids: the cursor, the arithmetic, and the two page heads.
 *
 * <p>Deliberately not general — the layout is fixed at 36 entries per page, content from slot 9,
 * back at 45, forward at 53. A second layout should add a factory here, not four constructor
 * arguments. It does not own re-rendering either: the menu hands in its own {@code updateInventory}.
 */
final class GridPaging {

    static final int ENTRIES_PER_PAGE = 36;
    static final int FIRST_CONTENT_SLOT = 9;
    static final int PREVIOUS_SLOT = 45;
    static final int NEXT_SLOT = 53;

    private int currentPage;

    @FunctionalInterface
    interface SlotVisitor {
        void accept(int index, int slot);
    }

    /** Call where a menu changes what it is showing. */
    void reset() {
        this.currentPage = 0;
    }

    int currentPage() {
        return this.currentPage;
    }

    /** Always at least one, even for no entries. */
    static int pageCount(int total) {
        return Math.max(1, (int) Math.ceil((double) total / ENTRIES_PER_PAGE));
    }

    /**
     * Visits every entry belonging to the current page. The slot mapping is applied rather than
     * handed back, so a menu can only ask what goes in a slot.
     *
     * @param visit receives the index into the caller's list, and the slot to draw it in
     */
    void forEachOnPage(int total, SlotVisitor visit) {
        int startIndex = this.currentPage * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, total);

        for (int index = startIndex; index < endIndex; index++) {
            visit.accept(index, index - startIndex + FIRST_CONTENT_SLOT);
        }
    }

    /**
     * Draws the two page heads, or nothing when everything fits on one page. Both are always drawn
     * together: the one that cannot be used shows disabled and buzzes rather than disappearing.
     *
     * @param onPageChanged the menu's own redraw, run after the cursor moves
     */
    void draw(InventoryBuilder inventory, int total, Runnable onPageChanged) {
        if (pageCount(total) <= 1) {
            return;
        }

        boolean hasPrevious = this.currentPage > 0;
        boolean hasNext = this.currentPage < pageCount(total) - 1;

        inventory.setItem(PREVIOUS_SLOT, GuiItems.pageBack(hasPrevious),
                event -> this.turn(inventory, hasPrevious, -1, onPageChanged));
        inventory.setItem(NEXT_SLOT, GuiItems.pageForward(hasNext),
                event -> this.turn(inventory, hasNext, 1, onPageChanged));
    }

    private void turn(InventoryBuilder inventory, boolean allowed, int delta, Runnable onPageChanged) {
        if (!allowed) {
            inventory.getPlayer().playSound(inventory.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            return;
        }

        inventory.getPlayer().playSound(inventory.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
        this.currentPage += delta;
        onPageChanged.run();
    }
}
