package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.achievements.Achievements;
import forceitembattle.model.stats.AchievementUnlock;
import forceitembattle.model.stats.PlayerIdentity;
import forceitembattle.achievements.CollectionRule;
import forceitembattle.collection.CollectedItem;
import forceitembattle.achievements.global.GlobalRule;
import forceitembattle.achievements.global.GlobalStats;
import forceitembattle.util.ProgressBar;
import forceitembattle.util.Text;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public final class AchievementInventory extends InventoryBuilder {

    private static final DateTimeFormatter WHEN_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final int BAR_WIDTH = 24;

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    private final AchievementScope scope;
    /** Only the achievements of this scope, in declaration order. Paging is over this, not values(). */
    private final List<Achievements> entries;
    private final GridPaging paging = new GridPaging();
    // achievementId -> its unlock records (SOLO/TEAM), fetched from the service for display.
    private Map<String, List<AchievementUnlock>> unlocks = new HashMap<>();
    // Only fetched for the GLOBAL scope; null until it lands.
    private GlobalStats globalStats;
    // Found-set for the COLLECTION page's progress bars; null until loaded.
    private Map<String, CollectedItem> foundItems;

    public AchievementInventory(ForceItemBattle plugin, String playerName, UUID playerUUID, AchievementScope scope) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>" + scope.getDisplayName() + " <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.scope = scope;
        this.entries = Arrays.stream(Achievements.values())
                .filter(achievement -> achievement.getScope() == scope)
                .toList();

        this.addUpdateHandler(this::updateInventory);

        // Pull the full unlock records (mode + teammate + unlockedAt) from the service — the local
        // cache only holds ids — and refresh once they arrive. Each record carries its teammate's
        // name, so this one round trip is everything the menu needs.
        this.plugin.getFibService().achievements().unlocks(playerUUID,
                loaded -> {
                    this.unlocks = indexByAchievementId(loaded);
                    this.updateInventory();
                },
                error -> {
                    this.plugin.getLogger().warning("Failed to load achievement details for "
                            + playerUUID + " (HTTP " + error.getCode() + "): " + error.getMessage());
                    this.updateInventory();
                });

        // Progress on locked entries is only meaningful for GLOBAL — a ROUND achievement's
        // progress is in-memory and per-round, and META progress is counted locally below.
        if (scope == AchievementScope.GLOBAL) {
            this.plugin.getAchievementManager().getGlobalStatsLoader().load(playerUUID, stats -> {
                this.globalStats = stats;
                this.updateInventory();
            });
        } else if (scope == AchievementScope.COLLECTION) {
            this.plugin.getCollectionManager().getFoundItemsLoader().load(playerUUID, found -> {
                this.foundItems = found;
                this.updateInventory();
            });
        }

        // Read-only display: cancel every click so the shown dyes can't be taken.
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    /** An achievement can be unlocked more than once — solo, and once per teammate. */
    private Map<String, List<AchievementUnlock>> indexByAchievementId(List<AchievementUnlock> unlocks) {
        Map<String, List<AchievementUnlock>> map = new HashMap<>();
        for (AchievementUnlock entry : unlocks) {
            map.computeIfAbsent(entry.achievementId(), key -> new ArrayList<>()).add(entry);
        }
        return map;
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        // Fallback completion source: the local id cache (covers a just-unlocked
        // achievement whose async service write hasn't landed yet).
        Set<String> cachedIds = this.plugin.getAchievementManager()
                .getAchievementStorage().getPlayerAchievements(this.playerUUID);

        this.setItem(49, GuiItems.back(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new AchievementCategoryInventory(this.plugin, this.playerName, this.playerUUID)
                            .open(this.getPlayer());
                });

        this.paging.draw(this, this.entries.size(), this::updateInventory);

        this.paging.forEachOnPage(this.entries.size(), (i, slotIndex) -> {
            Achievements achievement = this.entries.get(i);

            List<AchievementUnlock> unlockRecords = this.unlocks.get(achievement.name());
            boolean isCompleted = isCompleted(achievement, cachedIds);

            Material displayMaterial = isCompleted ? Material.LIME_DYE : Material.GRAY_DYE;
            String displayName = isCompleted
                    ? "<dark_gray>» <green>✔ <dark_aqua>" + achievement.getTitle()
                    : "<dark_gray>» <gray>✘ <dark_aqua>" + achievement.getTitle();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>" + achievement.getDescription());
            lore.add("");

            if (isCompleted) {
                lore.add("<green>Completed!");
                if (unlockRecords != null) {
                    for (AchievementUnlock entry : unlockRecords) {
                        if (entry.inTeam()) {
                            lore.add("<dark_gray>» <aqua>Team <dark_gray>◆ <gray>with <yellow>"
                                    + PlayerIdentity.displayName(entry.teammate(), "Unknown"));
                        } else {
                            lore.add("<dark_gray>» <aqua>Solo");
                        }
                        String when = formatWhen(entry.unlockedAt());
                        if (when != null) {
                            lore.add("<dark_gray>» <gray>" + when);
                        }
                    }
                }
            } else {
                lore.add("<gray>Not completed yet");
                lore.addAll(progressLore(achievement, cachedIds));
            }
            lore.add("");

            boolean isDex = achievement.getScope() == AchievementScope.COLLECTION;
            if (isDex) {
                lore.add("<yellow>Click to view your collection");
                lore.add("");
            }

            ItemStack stack = new ItemBuilder(displayMaterial)
                    .setDisplayName(displayName)
                    .setLore(lore)
                    .getItemStack();

            if (isDex) {
                this.setItem(slotIndex, stack, inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new CollectionBookInventory(this.plugin, this.playerName, this.playerUUID).open(this.getPlayer());
                });
            } else {
                this.setItem(slotIndex, stack);
            }
        });
    }

    private boolean isCompleted(Achievements achievement, Set<String> cachedIds) {
        List<AchievementUnlock> records = this.unlocks.get(achievement.name());
        return (records != null && !records.isEmpty()) || cachedIds.contains(achievement.name());
    }

    /**
     * Progress toward a locked achievement, where that is a meaningful thing to show.
     *
     * <p>GLOBAL reads its current value off the fetched stats and its target off the rule — the
     * same rule the unlock check uses, so the bar cannot disagree with reality. META counts its
     * required achievements locally. ROUND gets nothing: its progress only exists in memory
     * during a round, and showing a stale zero would be worse than showing nothing.
     */
    private List<String> progressLore(Achievements achievement, Set<String> cachedIds) {
        List<String> lore = new ArrayList<>();

        if (achievement.getScope() == AchievementScope.COLLECTION) {
            Set<String> catalogue = this.plugin.getCollectionManager().getCollectionCatalogue();
            if (this.foundItems == null) {
                lore.add("<dark_gray>» <gray>Loading progress...");
                return lore;
            }
            CollectionRule rule = achievement.getCollectionRule();
            long required = rule.requiredCount(catalogue.size());
            long current = rule.collectedCount(this.foundItems.keySet(), catalogue);
            lore.add("<dark_gray>» <dark_aqua>" + current + " <gray>/ <dark_aqua>" + required + " <gray>items collected");
            lore.add("<dark_gray>» " + ProgressBar.of(current, required));
            return lore;
        }

        if (achievement.getScope() == AchievementScope.GLOBAL) {
            GlobalRule rule = achievement.getGlobalRule();
            if (this.globalStats == null) {
                lore.add("<dark_gray>» <gray>Loading progress...");
                return lore;
            }
            long current = Math.min(this.globalStats.get(rule.stat()), rule.threshold());
            lore.add("<dark_gray>» <dark_aqua>" + current + " <gray>/ <dark_aqua>" + rule.threshold()
                    + " <gray>" + rule.stat().getLabel());
            lore.add("<dark_gray>» " + progressBar(current, rule.threshold()));
            return lore;
        }

        if (achievement.getScope() == AchievementScope.META) {
            Set<AchievementScope> required = achievement.getCompletionistRule().requiredScopes();

            int total = 0;
            int done = 0;
            for (Achievements other : Achievements.values()) {
                if (!required.contains(other.getScope())) {
                    continue;
                }
                total++;
                if (isCompleted(other, cachedIds)) {
                    done++;
                }
            }

            lore.add("<dark_gray>» <dark_aqua>" + done + " <gray>/ <dark_aqua>" + total + " <gray>required achievements");
            lore.add("<dark_gray>» " + progressBar(done, total));
            return lore;
        }

        return lore;
    }

    private String progressBar(long current, long target) {
        double ratio = target <= 0 ? 1.0 : Math.clamp((double) current / target, 0.0, 1.0);
        double pct = Math.round(ratio * 1000) / 10.0;
        int filled = (int) Math.round(ratio * BAR_WIDTH);
        if (pct >= 100.0) filled = BAR_WIDTH;
        else if (pct > 0.0) filled = Math.clamp(filled, 1, BAR_WIDTH - 1);
        return "<dark_gray><st><green>" + " ".repeat(filled) + "</st>"
                + "<dark_gray><st>" + " ".repeat(BAR_WIDTH - filled) + "</st>"
                + " <yellow>" + pct + "%";
    }


    private String formatWhen(OffsetDateTime when) {
        if (when == null) {
            return null;
        }
        return when.atZoneSameInstant(ZoneId.systemDefault()).format(WHEN_FORMAT);
    }
}
