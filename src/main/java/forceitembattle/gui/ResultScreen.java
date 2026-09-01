package forceitembattle.gui;

import forceitembattle.model.ScoreOwner;
import forceitembattle.util.Text;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

/**
 * A result screen reopened after the fact, from pages the reveal already built.
 *
 * <p>The other half of what {@code FinishInventory} did behind its {@code firstTime} boolean. This
 * one builds nothing and deals nothing out: it renders pages it is handed, and pages them.
 *
 * <p>The pages arrive as a parameter rather than being fetched here. {@code FinishInventory} read
 * them out of {@code Gamemanager} itself, which is what made the archive reachable — and writable —
 * from inside a GUI.
 */
public final class ResultScreen extends InventoryBuilder {

    private static final int NEXT_PAGE_SLOT = 35;
    private static final int PREVIOUS_PAGE_SLOT = 27;
    private static final int FIRST_ITEM_SLOT = 10;
    private static final int LAST_ITEM_SLOT = 53;

    public ResultScreen(ScoreOwner owner, @Nullable Map<Integer, Map<Integer, ItemStack>> pages) {
        super(9 * 6, Text.of("<dark_gray>» <gold>Items <dark_gray>● <gray>"
                + ResultDisplay.windowTitleFor(owner)));

        this.setItems(0, 8, GuiItems.border());
        this.setItems(9, LAST_ITEM_SLOT, GuiItems.filler());

        AtomicInteger currentPage = new AtomicInteger(0);
        this.addUpdateHandler(() -> this.renderPage(pages, currentPage));

        this.addClickHandler(event -> event.setCancelled(true));
    }

    private void renderPage(@Nullable Map<Integer, Map<Integer, ItemStack>> pages, AtomicInteger currentPage) {
        if (pages == null || pages.isEmpty()) {
            this.setItem(FIRST_ITEM_SLOT, GuiItems.noItemsFound());
            return;
        }

        this.setItems(9, LAST_ITEM_SLOT, GuiItems.filler());
        this.placeItems(pages.get(currentPage.get()));

        if (pages.size() <= 1) {
            return;
        }

        if (currentPage.get() != pages.size() - 1) {
            this.setItem(NEXT_PAGE_SLOT, GuiItems.nextPage(), event -> this.turnPage(currentPage, 1));
        }
        if (currentPage.get() != 0) {
            this.setItem(PREVIOUS_PAGE_SLOT, GuiItems.previousPage(), event -> this.turnPage(currentPage, -1));
        }
    }

    private void turnPage(AtomicInteger currentPage, int delta) {
        currentPage.addAndGet(delta);

        getPlayer().playSound(getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
    }

    private void placeItems(@Nullable Map<Integer, ItemStack> itemStacksPerPage) {
        if (itemStacksPerPage != null) {
            itemStacksPerPage.forEach(this::setItem);
        }
    }
}
