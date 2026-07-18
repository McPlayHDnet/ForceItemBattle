package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.collection.CollectedItem;
import forceitembattle.collection.CollectionCategory;
import forceitembattle.util.ProgressBar;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Landing page of the collection book: one head per {@link CollectionCategory}, each showing how
 * much of that category the player has collected. Clicking a category opens its item grid.
 *
 * The collection is loaded once here (cached) and re-used for every category count; the category
 * page is a cache hit off the same load, so the whole book costs one read.
 */
public class CollectionBookInventory extends InventoryBuilder {

    // Usable slots (rows 2-5), avoiding the top border row and the bottom control row.
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
    };

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    // null until the collection lands.
    private Map<String, CollectedItem> collected;

    public CollectionBookInventory(ForceItemBattle plugin, String playerName, UUID playerUUID) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>Collection <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;

        this.addUpdateHandler(this::updateInventory);
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));

        this.plugin.getCollectionManager().getFoundItemsLoader().load(playerUUID, found -> {
            this.collected = found;
            this.updateInventory();
        });
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        Map<CollectionCategory, List<Material>> buckets = this.plugin.getCollectionManager().getCollectionBuckets();

        // Counted once per category and reused below: the render loop used to re-scan every
        // bucket, walking the whole ~1300-item catalogue twice per repaint.
        Map<CollectionCategory, Integer> foundPerCategory = new EnumMap<>(CollectionCategory.class);
        int overallTotal = 0;
        int overallFound = 0;
        for (Map.Entry<CollectionCategory, List<Material>> entry : buckets.entrySet()) {
            int found = countCollected(entry.getValue());
            foundPerCategory.put(entry.getKey(), found);
            overallTotal += entry.getValue().size();
            overallFound += found;
        }

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");
        if (this.collected == null) {
            summaryLore.add("<gray>Loading your collection...");
        } else {
            summaryLore.add("<dark_gray>» <dark_aqua>" + overallFound + " <gray>/ <dark_aqua>" + overallTotal + " <gray>items collected");
            summaryLore.add("<dark_gray>» " + ProgressBar.of(overallFound, overallTotal));
        }
        this.setItem(4, new ItemBuilder(Material.BUNDLE)
                .setDisplayName("<dark_gray>» <dark_aqua>Your Collection")
                .setLore(summaryLore)
                .getItemStack());

        this.setItem(49, new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYwNDhmMThmYzgwMzQ3NWY3In19fQ==")
                        .setDisplayName("<dark_red>« <red>Back")
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new AchievementInventory(this.plugin, this.playerName, this.playerUUID, AchievementScope.GLOBAL)
                            .open(this.getPlayer());
                });

        CollectionCategory[] categories = CollectionCategory.values();
        for (int i = 0; i < categories.length && i < CONTENT_SLOTS.length; i++) {
            CollectionCategory category = categories[i];
            List<Material> items = buckets.getOrDefault(category, List.of());

            int total = items.size();
            int found = foundPerCategory.getOrDefault(category, 0);
            boolean complete = this.collected != null && total > 0 && found == total;

            List<String> lore = new ArrayList<>();
            lore.add("");
            if (this.collected == null) {
                lore.add("<gray>Loading...");
            } else {
                lore.add("<dark_gray>» <dark_aqua>" + found + " <gray>/ <dark_aqua>" + total + " <gray>collected");
                lore.add("<dark_gray>» " + ProgressBar.of(found, total));
                lore.add("");
                lore.add("<yellow>Click to view");
            }

            this.setItem(CONTENT_SLOTS[i], category.head()
                            .setGlowing(complete)
                            .setDisplayName("<dark_gray>» <dark_aqua>" + category.getDisplayName()
                                    + (complete ? " <green>✔" : ""))
                            .setLore(lore)
                            .getItemStack(),
                    inventoryClickEvent -> {
                        this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                        new CollectionDexInventory(this.plugin, this.playerName, this.playerUUID, category)
                                .open(this.getPlayer());
                    });
        }
    }

    private int countCollected(List<Material> items) {
        if (this.collected == null) {
            return 0;
        }
        return (int) items.stream()
                .filter(material -> this.collected.containsKey(material.getKey().asString()))
                .count();
    }
}
