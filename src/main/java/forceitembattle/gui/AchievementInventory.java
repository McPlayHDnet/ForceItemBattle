package forceitembattle.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerAchievementsDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.global.GlobalRule;
import forceitembattle.achievements.global.GlobalStats;
import forceitembattle.util.Text;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;

public class AchievementInventory extends InventoryBuilder {

    private static final DateTimeFormatter WHEN_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final int BAR_WIDTH = 20;

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;
    private final AchievementScope scope;
    /** Only the achievements of this scope, in declaration order. Paging is over this, not values(). */
    private final List<Achievements> entries;
    // Resolved teammate names (from local cache or Mojang), so each UUID is looked up once.
    private final Map<UUID, String> nameCache = new HashMap<>();
    private final Set<UUID> nameLookupsInFlight = new HashSet<>();
    private int currentPage;
    // achievementId -> its unlock records (SOLO/TEAM), fetched from the service for display.
    private Map<String, List<FibAchievementDto>> unlocks = new HashMap<>();
    // Only fetched for the GLOBAL scope; null until it lands.
    private GlobalStats globalStats;

    public AchievementInventory(ForceItemBattle plugin, String playerName, UUID playerUUID, AchievementScope scope) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>" + scope.getDisplayName() + " <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.scope = scope;
        this.currentPage = 0;
        this.entries = Arrays.stream(Achievements.values())
                .filter(achievement -> achievement.getScope() == scope)
                .toList();

        this.addUpdateHandler(this::updateInventory);

        // Pull the full unlock records (mode + teammate + unlockedAt) from the
        // service — the local cache only holds ids — and refresh once they arrive.
        this.plugin.getFibService().achievements().getPlayerAchievementsAsync(playerUUID,
                dto -> {
                    this.unlocks = indexByAchievementId(dto);
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
        }

        // Read-only display: cancel every click so the shown dyes can't be taken.
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    private Map<String, List<FibAchievementDto>> indexByAchievementId(FibPlayerAchievementsDto dto) {
        Map<String, List<FibAchievementDto>> map = new HashMap<>();
        if (dto != null && dto.getAchievements() != null) {
            for (FibAchievementDto entry : dto.getAchievements()) {
                map.computeIfAbsent(entry.getAchievementId(), key -> new ArrayList<>()).add(entry);
            }
        }
        return map;
    }

    private int totalPages(int objectsPerPage) {
        return Math.max(1, (int) Math.ceil((double) this.entries.size() / objectsPerPage));
    }

    private String getPageButton(int slot, int currentPage, int objectsPerPage) {
        String headValue = "";
        if (slot == 45) {
            if (currentPage == 0) {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=";
            } else {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
            }
        } else if (slot == 53) {
            if (currentPage == this.totalPages(objectsPerPage) - 1) {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=";
            } else {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19";
            }
        }
        return headValue;
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(45, 53, GuiItems.accentBorder());

        // Fallback completion source: the local id cache (covers a just-unlocked
        // achievement whose async service write hasn't landed yet).
        Set<String> cachedIds = this.plugin.getAchievementManager()
                .getAchievementStorage().getPlayerAchievements(this.playerUUID);

        int itemsPerPage = 36;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, this.entries.size());

        this.setItem(49, new ItemBuilder(Material.BARRIER)
                        .setDisplayName("<dark_red>« <red>Back")
                        .getItemStack(),
                inventoryClickEvent -> {
                    this.getPlayer().playSound(this.getPlayer(), Sound.UI_BUTTON_CLICK, 1, 1);
                    new AchievementCategoryInventory(this.plugin, this.playerName, this.playerUUID)
                            .open(this.getPlayer());
                });

        if (this.entries.size() > itemsPerPage) {
            this.setItem(45, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture(this.getPageButton(45, this.currentPage, itemsPerPage))
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
                    }
            );

            this.setItem(53, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture(this.getPageButton(53, this.currentPage, itemsPerPage))
                            .setDisplayName("<dark_green>» <green>Next page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if (this.currentPage < this.totalPages(itemsPerPage) - 1) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage++;
                            this.updateInventory();
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        }
                    }
            );
        }

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Achievements achievement = this.entries.get(i);

            List<FibAchievementDto> unlockRecords = this.unlocks.get(achievement.name());
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
                    for (FibAchievementDto entry : unlockRecords) {
                        if ("TEAM".equalsIgnoreCase(String.valueOf(entry.getMode()))) {
                            lore.add("<dark_gray>» <aqua>Team <dark_gray>◆ <gray>with <yellow>" + teammateName(entry.getTeammateUuid()));
                        } else {
                            lore.add("<dark_gray>» <aqua>Solo");
                        }
                        String when = formatWhen(entry.getUnlockedAt());
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

            this.setItem(slotIndex, new ItemBuilder(displayMaterial)
                    .setDisplayName(displayName)
                    .setLore(lore)
                    .getItemStack());
        }
    }

    private boolean isCompleted(Achievements achievement, Set<String> cachedIds) {
        List<FibAchievementDto> records = this.unlocks.get(achievement.name());
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
        int filled = target <= 0 ? BAR_WIDTH : (int) Math.clamp(current * BAR_WIDTH / target, 0, BAR_WIDTH);
        return "<green>" + "▉".repeat(filled) + "<dark_gray>" + "▉".repeat(BAR_WIDTH - filled);
    }

    private String teammateName(UUID teammateUuid) {
        if (teammateUuid == null) {
            return "Unknown";
        }
        String cached = this.nameCache.get(teammateUuid);
        if (cached != null) {
            return cached;
        }
        String name = Bukkit.getOfflinePlayer(teammateUuid).getName();
        if (name != null) {
            this.nameCache.put(teammateUuid, name);
            return name;
        }
        // Unknown locally — look it up from Mojang off-thread, then refresh the menu.
        // Show the short UUID as a placeholder until it resolves.
        resolveNameAsync(teammateUuid);
        return teammateUuid.toString().substring(0, 8);
    }

    private void resolveNameAsync(UUID teammateUuid) {
        if (!this.nameLookupsInFlight.add(teammateUuid)) {
            return; // a lookup for this UUID is already running
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            String resolved = null;
            try {
                PlayerProfile profile = Bukkit.createProfile(teammateUuid);
                profile.complete(false); // fills name + uuid from cache/Mojang, skips textures
                resolved = profile.getName();
            } catch (Exception ignored) {
                // network/lookup failure — fall back to the short UUID below
            }
            final String name = resolved;
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.nameLookupsInFlight.remove(teammateUuid);
                this.nameCache.put(teammateUuid, (name != null && !name.isEmpty())
                        ? name
                        : teammateUuid.toString().substring(0, 8));
                this.updateInventory();
            });
        });
    }

    private String formatWhen(OffsetDateTime when) {
        if (when == null) {
            return null;
        }
        return when.atZoneSameInstant(ZoneId.systemDefault()).format(WHEN_FORMAT);
    }
}
