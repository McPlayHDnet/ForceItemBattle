package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.achievements.global.CollectedItem;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Landing page of the collection book: one entry per {@link CollectionCategory}, each showing how
 * many of that category's items the player has collected. Clicking a category opens its item grid.
 *
 * The found-set is loaded once here (cached) and re-used for every category count; the category
 * item view is a cache hit off the same load, so the whole book costs one read.
 */
public class CollectionBookInventory extends InventoryBuilder {

    // Usable slots (rows 2-4), avoiding the top border row and the bottom control row.
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
    };

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    // null until the found-set lands.
    private Map<String, CollectedItem> foundItems;

    public CollectionBookInventory(ForceItemBattle plugin, String playerName, UUID playerUUID) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>Collection <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;

        this.addUpdateHandler(this::updateInventory);
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));

        this.plugin.getAchievementManager().getFoundItemsLoader().load(playerUUID, found -> {
            this.foundItems = found;
            this.updateInventory();
        });
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        Map<CollectionCategory, List<Material>> buckets = this.plugin.getAchievementManager().getCollectionBuckets();

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
            int found = this.foundItems == null ? 0 : (int) items.stream()
                    .filter(material -> this.foundItems.containsKey(material.getKey().asString()))
                    .count();
            double percent = total == 0 ? 0.0 : Math.round((double) found / total * 1000) / 10.0;

            List<String> lore = new ArrayList<>();
            lore.add("");
            if (this.foundItems == null) {
                lore.add("<gray>Loading...");
            } else {
                lore.add("<dark_gray>» <dark_aqua>" + found + " <gray>/ <dark_aqua>" + total + " <gray>collected");
                lore.add("<dark_gray>» <yellow>" + percent + "%");
                lore.add("");
                lore.add("<yellow>Click to view");
            }

            ItemBuilder builder = category.getHeadTexture().isEmpty()
                    ? new ItemBuilder(category.getIcon())
                    : new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture(category.getHeadTexture());

            this.setItem(CONTENT_SLOTS[i], builder
                            .setDisplayName("<dark_gray>» <dark_aqua>" + category.getDisplayName())
                            .setLore(lore)
                            .getItemStack(),
                    inventoryClickEvent -> {
                        this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                        new CollectionDexInventory(this.plugin, this.playerName, this.playerUUID, category)
                                .open(this.getPlayer());
                    });
        }
    }
}
