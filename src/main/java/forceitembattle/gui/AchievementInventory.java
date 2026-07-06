package forceitembattle.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.threeseconds.openapi.fibservice.client.model.FibAchievementDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerAchievementsDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Achievements;
import forceitembattle.util.Text;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

public class AchievementInventory extends InventoryBuilder {

    private static final DateTimeFormatter WHEN_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ForceItemBattle plugin;
    private final UUID playerUUID;
    // Resolved teammate names (from local cache or Mojang), so each UUID is looked up once.
    private final Map<UUID, String> nameCache = new HashMap<>();
    private final Set<UUID> nameLookupsInFlight = new HashSet<>();
    private int currentPage;
    // achievementId -> its unlock records (SOLO/TEAM), fetched from the service for display.
    private Map<String, List<FibAchievementDto>> unlocks = new HashMap<>();

    public AchievementInventory(ForceItemBattle plugin, String playerName, UUID playerUUID) {
        super(9 * 6, Text.of("<dark_gray>» <dark_aqua>Achievements <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.currentPage = 0;
        this.playerUUID = playerUUID;

        this.addUpdateHandler(this::updateInventory);

        // Pull the full unlock records (mode + teammate + unlockedAt) from the
        // service — the local cache only holds ids — and refresh once they arrive.
        this.plugin.getFibServiceHelper().getPlayerAchievementsAsync(playerUUID,
                dto -> {
                    this.unlocks = indexByAchievementId(dto);
                    this.updateInventory();
                },
                error -> {
                    this.plugin.getLogger().warning("Failed to load achievement details for "
                            + playerUUID + " (HTTP " + error.getCode() + "): " + error.getMessage());
                    this.updateInventory();
                });

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
        return (int) Math.ceil((double) Achievements.values().length / objectsPerPage);
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
        // Clear inventory
        this.getInventory().clear();

        // Top and bottom borders
        this.setItems(0, 8, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setDisplayName(" ").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setDisplayName(" ").addItemFlags(ItemFlag.values()).getItemStack());

        // Fallback completion source: the local id cache (covers a just-unlocked
        // achievement whose async service write hasn't landed yet).
        Set<String> cachedIds = this.plugin.getAchievementManager().getAchievementStorage().getPlayerAchievements(this.playerUUID);

        int achievementSize = Achievements.values().length;
        int itemsPerPage = 36;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage - 1, achievementSize - 1);

        // Page buttons
        if (achievementSize > itemsPerPage) {
            this.setItem(45, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture(this.getPageButton(45, this.currentPage, itemsPerPage))
                            .setDisplayName("<dark_red>« <red>Previous page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if (this.currentPage > 0) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage--;
                            this.updateInventory(); // Re-render the inventory
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
                            this.updateInventory(); // Re-render the inventory
                        } else {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                        }
                    }
            );
        }

        // Display achievements for current page
        for (int i = startIndex; i <= endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Achievements achievement = Achievements.values()[i];

            List<FibAchievementDto> entries = this.unlocks.get(achievement.name());
            boolean isCompleted = (entries != null && !entries.isEmpty()) || cachedIds.contains(achievement.name());

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
                if (entries != null) {
                    for (FibAchievementDto entry : entries) {
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
            }
            lore.add("");

            this.setItem(slotIndex, new ItemBuilder(displayMaterial)
                    .setDisplayName(displayName)
                    .setLore(lore)
                    .getItemStack());
        }
    }

    private String teammateName(UUID teammateUuid) {
        if (teammateUuid == null) {
            return "Unknown";
        }
        // Already resolved (locally or from a prior Mojang lookup).
        String cached = this.nameCache.get(teammateUuid);
        if (cached != null) {
            return cached;
        }
        // Local server cache (online players and anyone recently seen).
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
                // Cache the resolved name, or the short UUID so we don't keep retrying.
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
