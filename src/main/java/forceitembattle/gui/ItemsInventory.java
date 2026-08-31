package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.util.Text;
import java.util.HashMap;
import java.util.function.BiPredicate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public final class ItemsInventory extends InventoryBuilder {

    private static final int FIRST_CONTENT_SLOT = 9;
    private static final int FIRST_FILTER_SLOT = 2;

    private final ForceItemBattle forceItemBattle;
    private final HashMap<Integer, HashMap<Integer, ItemStack>> pages = new HashMap<>();
    private final int[] currentPage = {0};
    private Filter currentFilter = Filter.ALL;

    /** The five views of the item catalogue this menu offers. */
    private enum Filter {
        ALL(Material.LIME_DYE, "<green>All items",
                (items, material) -> true),
        INCLUDED(Material.ORANGE_DYE, "<gold>Included Items",
                ItemDifficultiesManager::itemInList),
        NON_CRAFTABLE(Material.RED_DYE, "<red>All non-craftable",
                (items, material) -> Bukkit.getRecipesFor(new ItemStack(material)).isEmpty()),
        DESCRIBED(Material.LIGHT_BLUE_DYE, "<aqua>Included Items with description",
                (items, material) -> items.itemInAllLists(material) && items.itemHasDescription(material)),
        EXCLUDED(Material.GRAY_DYE, "<gray>Excluded Items",
                (items, material) -> !items.itemInAllLists(material));

        static final Filter[] VALUES = values();

        private final Material icon;
        private final String displayName;
        private final BiPredicate<ItemDifficultiesManager, Material> test;

        Filter(Material icon, String displayName, BiPredicate<ItemDifficultiesManager, Material> test) {
            this.icon = icon;
            this.displayName = displayName;
            this.test = test;
        }

        boolean accepts(ItemDifficultiesManager items, Material material) {
            return this.test.test(items, material);
        }

        /**
         * Whether entries carry the "registered" glow and their description lore. Excluded items are
         * by definition in neither, so they are drawn bare.
         */
        boolean detailed() {
            return this != EXCLUDED;
        }
    }

    public ItemsInventory(ForceItemBattle forceItemBattle, Player player) {
        super(9 * 6, Text.of("<dark_gray>» <gold>Items <gray>("
                + forceItemBattle.getItemDifficultiesManager().getAllItems().size()
                + ") <dark_gray>● <gray>Settings"));

        this.forceItemBattle = forceItemBattle;

        /* TOP-BORDER */
        this.setItems(0, 8, GuiItems.border());

        drawFilterButtons(player);

        this.setItem(0, GuiItems.previousPage(), inventoryClickEvent -> {
            if (currentPage[0] != 0) {
                currentPage[0]--;

                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);

                pages.get(currentPage[0]).forEach(this::setItem);

            } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
        });
        this.setItem(8, GuiItems.nextPage(), inventoryClickEvent -> {
            if (currentPage[0] < (pages.size() - 1)) {
                currentPage[0]++;

                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);

                clearContent();
                pages.get(currentPage[0]).forEach(this::setItem);

            } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
        });

        this.setFilteredItems();

        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    /** Must be redrawn on every filter switch, or the glow stays on whichever view was built first. */
    private void drawFilterButtons(Player player) {
        for (Filter filter : Filter.VALUES) {
            this.setItem(FIRST_FILTER_SLOT + filter.ordinal(),
                    new ItemBuilder(filter.icon)
                            .setDisplayName(filter.displayName)
                            .setGlowing(this.currentFilter == filter)
                            .addItemFlags(ItemFlag.values())
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if (this.currentFilter == filter) return;
                        this.currentFilter = filter;
                        this.currentPage[0] = 0;

                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                        drawFilterButtons(player);
                        clearContent();
                        this.setFilteredItems();
                    });
        }
    }

    private void clearContent() {
        this.setItems(FIRST_CONTENT_SLOT, this.getInventory().getSize() - 1, new ItemStack(Material.AIR));
    }

    private void setFilteredItems() {
        ItemDifficultiesManager items = this.forceItemBattle.getItemDifficultiesManager();

        int slot = FIRST_CONTENT_SLOT;
        int page = 0;

        this.pages.clear();
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir()) {
                continue;
            }
            if (!this.currentFilter.accepts(items, material)) {
                continue;
            }

            this.pages.computeIfAbsent(page, key -> new HashMap<>())
                    .put(slot, icon(items, material));

            slot++;
            if (slot == this.getInventory().getSize()) {
                slot = FIRST_CONTENT_SLOT;
                page++;
            }
        }

        if (this.pages.isEmpty()) {
            this.pages.put(0, new HashMap<>());
        }

        HashMap<Integer, ItemStack> shown = this.pages.get(this.currentPage[0]);
        if (shown != null) {
            shown.forEach(this::setItem);
        } else {
            getPlayer().sendMessage("No items to display on this page.");
        }
    }

    private ItemStack icon(ItemDifficultiesManager items, Material material) {
        ItemBuilder builder = new ItemBuilder(material);
        if (this.currentFilter.detailed()) {
            builder.setGlowing(items.itemInAllLists(material))
                    .setLoreLegacy(items.getDescriptionItemLines(material));
        }
        return builder.addItemFlags(ItemFlag.values()).getItemStack();
    }
}
