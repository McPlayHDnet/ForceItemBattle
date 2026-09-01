package forceitembattle.gui;

import org.bukkit.Sound;

/**
 * Paging for the six-row collection grids: the cursor, the arithmetic, and the two page heads.
 *
 * <p>{@code CollectionDexInventory} and {@code AchievementInventory} each carried their own copy —
 * a bare {@code int currentPage} field, the same start/end/slot arithmetic, and forty lines of
 * button wiring that had to be kept in step by eye. They had already drifted in spelling: one
 * guarded on {@code totalPages() > 1}, the other on {@code entries.size() > itemsPerPage} (the same
 * condition), and the second held {@code itemsPerPage} as a local while its {@code totalPages}
 * took it as a parameter.
 *
 * <h2>Deliberately not general</h2>
 *
 * <p>The layout is fixed: 36 entries per page, content from slot 9, back at 45, forward at 53, in a
 * 6×9 inventory. Both callers use exactly those, and a parameter that every caller passes the same
 * value to hides a decision nobody has made. A second layout should add a factory here, not four
 * constructor arguments.
 *
 * <h2>What it does not own</h2>
 *
 * <p>Re-rendering. The menu hands in its own {@code updateInventory} as a {@link Runnable}: how a
 * menu redraws itself is its contract, not this one's.
 */
final class GridPaging {

    static final int ENTRIES_PER_PAGE = 36;
    static final int FIRST_CONTENT_SLOT = 9;
    static final int PREVIOUS_SLOT = 45;
    static final int NEXT_SLOT = 53;

    private int currentPage;

    /** What a menu does with one entry of the current page. */
    @FunctionalInterface
    interface SlotVisitor {
        void accept(int index, int slot);
    }

    /** Back to the first page. Called where a menu changes what it is showing. */
    void reset() {
        this.currentPage = 0;
    }

    int currentPage() {
        return this.currentPage;
    }

    /** How many pages {@code total} entries fill. Always at least one, even for none. */
    static int pageCount(int total) {
        return Math.max(1, (int) Math.ceil((double) total / ENTRIES_PER_PAGE));
    }

    /**
     * Visits every entry belonging to the current page, with the slot it goes in.
     *
     * <p>The mapping is applied rather than handed back, because {@code i - startIndex + 9} and the
     * clamp at the end of the last page are the arithmetic that gets copied wrong. A menu can only
     * ask what goes in a slot.
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
     * Draws the two page heads, or nothing at all when everything fits on one page.
     *
     * <p>Both heads are always drawn together once there is more than one page: the one that cannot
     * be used is drawn in its disabled form and buzzes when clicked, rather than disappearing.
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
