package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.global.CollectedItem;
import forceitembattle.model.CustomMaterials;
import forceitembattle.util.Text;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * One category's slice of the collection: found items glowing, missing ones plain, with a filter
 * (all / collected / missing) and a sort toggle. The item list comes from the memoized catalogue
 * buckets and the collection from the cached loader, so this can't disagree with the achievement
 * or with the book's counts.
 */
public class CollectionDexInventory extends InventoryBuilder {

    private static final int ITEMS_PER_PAGE = 36;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault());

    private enum Filter {
        ALL("All items"),
        COLLECTED("Collected only"),
        MISSING("Missing only");

        private final String displayName;

        Filter(String displayName) {
            this.displayName = displayName;
        }
    }

    private enum Sort {
        COLLECTED_FIRST("Collected first"),
        ALPHABETICAL("Alphabetical"),
        FIRST_OBTAINED("First obtained"),
        MOST_COLLECTED("Most collected");

        private final String displayName;

        Sort(String displayName) {
            this.displayName = displayName;
        }
    }

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    private final CollectionCategory category;
    private final List<Material> items;
    private int currentPage;
    private Filter filter = Filter.ALL;
    private Sort sort = Sort.COLLECTED_FIRST;
    // null until the collection lands.
    private Map<String, CollectedItem> collected;

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
            this.collected = found;
            this.updateInventory();
        });
    }

    private boolean isCollected(Material material) {
        return this.collected != null && this.collected.containsKey(material.getKey().asString());
    }

    private CollectedItem statsOf(Material material) {
        return this.collected == null ? null : this.collected.get(material.getKey().asString());
    }

    /** This category's items with the active filter applied and the active sort imposed. */
    private List<Material> visibleItems() {
        List<Material> visible = new ArrayList<>();
        for (Material material : this.items) {
            boolean found = isCollected(material);
            if (this.filter == Filter.COLLECTED && !found) {
                continue;
            }
            if (this.filter == Filter.MISSING && found) {
                continue;
            }
            visible.add(material);
        }

        // Ties (and every missing item under the metadata sorts) fall back to name order, so the
        // grid is stable rather than registry-ordered.
        Comparator<Material> byName = Comparator.comparing(material -> CustomMaterials.nameOf(material));
        Comparator<Material> comparator = switch (this.sort) {
            case ALPHABETICAL -> byName;
            case COLLECTED_FIRST -> Comparator.comparing((Material material) -> !isCollected(material)).thenComparing(byName);
            case FIRST_OBTAINED -> Comparator.comparing((Material material) -> {
                CollectedItem stats = statsOf(material);
                // Missing items sort last: no date to order them by.
                return stats == null || stats.firstCollected() == null ? Long.MAX_VALUE : stats.firstCollected().toEpochMilli();
            }).thenComparing(byName);
            case MOST_COLLECTED -> Comparator.comparing((Material material) -> {
                CollectedItem stats = statsOf(material);
                return stats == null ? 0L : -stats.timesCollected();
            }).thenComparing(byName);
        };
        visible.sort(comparator);
        return visible;
    }

    private int totalPages(int visibleCount) {
        return Math.max(1, (int) Math.ceil((double) visibleCount / ITEMS_PER_PAGE));
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        List<Material> visible = visibleItems();
        int total = this.items.size();
        int foundCount = (int) this.items.stream().filter(this::isCollected).count();
        double percent = total == 0 ? 0.0 : Math.round((double) foundCount / total * 1000) / 10.0;

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");
        if (this.collected == null) {
            summaryLore.add("<gray>Loading...");
        } else {
            summaryLore.add("<dark_gray>» <dark_aqua>" + foundCount + " <gray>/ <dark_aqua>" + total + " <gray>collected");
            summaryLore.add("<dark_gray>» <yellow>" + percent + "%");
        }
        this.setItem(4, new ItemBuilder(this.category.getIcon())
                .setDisplayName("<dark_gray>» <dark_aqua>" + this.category.getDisplayName())
                .setLore(summaryLore)
                .getItemStack());

        // --- controls ---
        this.setItem(47, new ItemBuilder(Material.HOPPER)
                        .setDisplayName("<dark_gray>» <dark_aqua>Filter<gray>: <yellow>" + this.filter.displayName)
                        .setLore(List.of("", "<yellow>Click to change"))
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    Filter[] filters = Filter.values();
                    this.filter = filters[(this.filter.ordinal() + 1) % filters.length];
                    this.currentPage = 0;
                    this.updateInventory();
                });

        this.setItem(51, new ItemBuilder(Material.COMPARATOR)
                        .setDisplayName("<dark_gray>» <dark_aqua>Sort<gray>: <yellow>" + this.sort.displayName)
                        .setLore(List.of("", "<yellow>Click to change"))
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    Sort[] sorts = Sort.values();
                    this.sort = sorts[(this.sort.ordinal() + 1) % sorts.length];
                    this.currentPage = 0;
                    this.updateInventory();
                });

        this.setItem(49, new ItemBuilder(Material.PLAYER_HEAD)
                        .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==")
                        .setDisplayName("<dark_red>« <red>Back")
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new CollectionBookInventory(this.plugin, this.playerName, this.playerUUID).open(this.getPlayer());
                });

        if (visible.size() > ITEMS_PER_PAGE) {
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
                        if (this.currentPage < this.totalPages(visible.size()) - 1) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage++;
                            this.updateInventory();
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        }
                    });
        }

        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, visible.size());
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Material material = visible.get(i);
            CollectedItem stats = statsOf(material);
            boolean found = stats != null;

            List<String> lore = new ArrayList<>();
            lore.add("");
            if (found) {
                lore.add("<dark_gray>» <green>✔ Collected");
                if (stats.firstCollected() != null) {
                    lore.add("<dark_gray>» <gray>First: <white>" + DATE_FORMAT.format(stats.firstCollected()));
                }
                lore.add("<dark_gray>» <gray>Collected <white>" + stats.timesCollected() + "<gray>x");
            } else {
                lore.add("<dark_gray>» <gray>✘ Not collected yet");
            }
            lore.add("");

            this.setItem(slotIndex, new ItemBuilder(material)
                    .setGlowing(found)
                    .setDisplayName((found ? "<green>" : "<gray>") + CustomMaterials.nameOf(material))
                    .setLore(lore)
                    .getItemStack());
        }
    }
}
