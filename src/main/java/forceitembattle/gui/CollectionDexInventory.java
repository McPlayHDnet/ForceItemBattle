package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * One category's slice of the collection: every item in the given {@link CollectionCategory},
 * found ones glowing, missing ones plain. Read-only, paged. The item list comes from the memoized
 * catalogue buckets and the found-set from the cached loader, so this can't disagree with the
 * achievement or with the book's counts.
 */
public class CollectionDexInventory extends InventoryBuilder {

    private static final int ITEMS_PER_PAGE = 36;

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    private final CollectionCategory category;
    private final List<Material> items;
    private int currentPage;
    // null until the found-set lands.
    private Set<String> foundItems;

    public CollectionDexInventory(ForceItemBattle plugin, String playerName, UUID playerUUID, CollectionCategory category) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>" + category.getDisplayName() + " <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.category = category;
        this.currentPage = 0;
        // Pre-bucketed and pre-sorted once by the manager; just read this category's list.
        this.items = this.plugin.getAchievementManager().getCollectionBuckets()
                .getOrDefault(category, List.of());

        this.addUpdateHandler(this::updateInventory);
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));

        this.plugin.getAchievementManager().getFoundItemsLoader().load(playerUUID, found -> {
            this.foundItems = found;
            this.updateInventory();
        });
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) this.items.size() / ITEMS_PER_PAGE));
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        int total = this.items.size();
        int foundCount = this.foundItems == null ? 0 : (int) this.items.stream()
                .filter(material -> this.foundItems.contains(material.getKey().asString()))
                .count();
        double percent = total == 0 ? 0.0 : Math.round((double) foundCount / total * 1000) / 10.0;

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");
        if (this.foundItems == null) {
            summaryLore.add("<gray>Loading...");
        } else {
            summaryLore.add("<dark_gray>» <dark_aqua>" + foundCount + " <gray>/ <dark_aqua>" + total + " <gray>collected");
            summaryLore.add("<dark_gray>» <yellow>" + percent + "%");
        }
        this.setItem(4, new ItemBuilder(this.category.getIcon())
                .setDisplayName("<dark_gray>» <dark_aqua>" + this.category.getDisplayName())
                .setLore(summaryLore)
                .getItemStack());

        this.setItem(49, new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==")
                        .setDisplayName("<dark_red>« <red>Back")
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new CollectionBookInventory(this.plugin, this.playerName, this.playerUUID).open(this.getPlayer());
                });

        if (this.items.size() > ITEMS_PER_PAGE) {
            this.setItem(45, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=")
                            .setDisplayName("<dark_red>« <red>Previous page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if (this.currentPage > 0) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage--;
                            this.updateInventory();
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        }
                    });

            this.setItem(53, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19")
                            .setDisplayName("<dark_green>» <green>Next page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if (this.currentPage < this.totalPages() - 1) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage++;
                            this.updateInventory();
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        }
                    });
        }

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.items.size());
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Material material = this.items.get(i);
            boolean found = this.foundItems != null && this.foundItems.contains(material.getKey().asString());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(found ? "<dark_gray>» <green>✔ Collected" : "<dark_gray>» <gray>✘ Not collected yet");
            lore.add("");

            this.setItem(slotIndex, new ItemBuilder(material)
                    .setGlowing(found)
                    .setDisplayName((found ? "<green>" : "<gray>") + prettify(material))
                    .setLore(lore)
                    .getItemStack());
        }
    }

    private static String prettify(Material material) {
        String[] parts = material.getKey().getKey().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
            }
        }
        return builder.toString().trim();
    }
}
