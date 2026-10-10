package forceitembattle.gui;

import org.bukkit.Sound;

/** Fixed layout: 36 entries per page from slot 9, back at 45, forward at 53. */
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

    /** @param visit receives the index into the caller's list, and the slot to draw it in */
    void forEachOnPage(int total, SlotVisitor visit) {
        int startIndex = this.currentPage * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, total);

        for (int index = startIndex; index < endIndex; index++) {
            visit.accept(index, index - startIndex + FIRST_CONTENT_SLOT);
        }
    }

    /** Both heads always drawn; an unusable one shows disabled. @param onPageChanged the menu's redraw */
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
