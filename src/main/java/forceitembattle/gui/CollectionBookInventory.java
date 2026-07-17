package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Item-collection dex: every item in the catalogue (EARLY∪MID∪LATE = {@code getAllItems()}),
 * found ones glowing, missing ones plain. Read-only, paged. Reads the same two sets the achievement
 * does -- the catalogue from ItemDifficultiesManager and the player's found-set from the
 * FoundItemsCache -- so what it shows can't disagree with the unlock.
 */
public class CollectionBookInventory extends InventoryBuilder {

    private static final int ITEMS_PER_PAGE = 36;

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    private final List<Material> catalogue;
    private int currentPage;
    // null until the found-set lands from the loader.
    private Set<String> foundItems;

    public CollectionBookInventory(ForceItemBattle plugin, String playerName, UUID playerUUID) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>Item Collection <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.currentPage = 0;

        // Materials come straight from the registry source; the found check compares namespaced keys.
        this.catalogue = new ArrayList<>(this.plugin.getItemDifficultiesManager().getAllItems());
        this.catalogue.sort(Comparator.comparing(material -> material.getKey().asString()));

        this.addUpdateHandler(this::updateInventory);
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));

        this.plugin.getAchievementManager().getFoundItemsLoader().load(playerUUID, found -> {
            this.foundItems = found;
            this.updateInventory();
        });
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) this.catalogue.size() / ITEMS_PER_PAGE));
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        int foundCount = this.foundItems == null ? 0 : (int) this.catalogue.stream()
                .filter(material -> this.foundItems.contains(material.getKey().asString()))
                .count();
        int total = this.catalogue.size();
        double percent = total == 0 ? 0.0 : Math.round((double) foundCount / total * 1000) / 10.0;

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");
        if (this.foundItems == null) {
            summaryLore.add("<gray>Loading your collection...");
        } else {
            summaryLore.add("<dark_gray>» <dark_aqua>" + foundCount + " <gray>/ <dark_aqua>" + total + " <gray>items collected");
            summaryLore.add("<dark_gray>» <yellow>" + percent + "%");
        }
        this.setItem(4, new ItemBuilder(Material.BUNDLE)
                .setDisplayName("<dark_gray>» <dark_aqua>Your Collection")
                .setLore(summaryLore)
                .getItemStack());

        this.setItem(49, new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==")
                        .setDisplayName("<dark_red>« <red>Back")
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new AchievementInventory(this.plugin, this.playerName, this.playerUUID, AchievementScope.GLOBAL)
                            .open(this.getPlayer());
                });

        if (this.catalogue.size() > ITEMS_PER_PAGE) {
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
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.catalogue.size());
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Material material = this.catalogue.get(i);
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
