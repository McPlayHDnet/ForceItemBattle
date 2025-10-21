package forceitembattle.settings.achievements;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemFlag;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class AchievementInventory extends InventoryBuilder {

    private final ForceItemBattle plugin;
    private int currentPage;
    private final UUID playerUUID;

    public AchievementInventory(ForceItemBattle plugin, String playerName, UUID playerUUID) {
        super(9 * 6, plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_aqua>Achievements <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.currentPage = 0;
        this.playerUUID = playerUUID;

        this.addUpdateHandler(this::updateInventory);
    }

    private int totalPages(int objectsPerPage) {
        return (int) Math.ceil((double) Achievements.values().length / objectsPerPage);
    }

    private String getPageButton(int slot, int currentPage, int objectsPerPage) {
        String headValue = "";
        if(slot == 45) {
            if(currentPage == 0) {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=";
            } else {
                headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
            }
        } else if(slot == 53) {
            if(currentPage == this.totalPages(objectsPerPage) - 1) {
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

        // Get player achievements
        Set<String> completedAchievements = this.plugin.getAchievementManager().getAchievementStorage().getPlayerAchievements(this.playerUUID);

        int achievementSize = Achievements.values().length;
        int itemsPerPage = 36;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage - 1, achievementSize - 1);

        // Page buttons
        if(achievementSize > itemsPerPage) {
            this.setItem(45, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture(this.getPageButton(45, this.currentPage, itemsPerPage))
                            .setDisplayName("<dark_red>« <red>Previous page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if(this.currentPage > 0) {
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
                        if(this.currentPage < this.totalPages(itemsPerPage) - 1) {
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
        for(int i = startIndex; i <= endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Achievements achievement = Achievements.values()[i];

            boolean isCompleted = completedAchievements.contains(achievement.name());
            Material displayMaterial = isCompleted ? Material.LIME_DYE : Material.GRAY_DYE;
            String displayName = isCompleted
                    ? "<dark_gray>» <green>✔ <dark_aqua>" + achievement.getTitle()
                    : "<dark_gray>» <gray>✘ <dark_aqua>" + achievement.getTitle();

            String statusLore = isCompleted ? "<green>Completed!" : "<gray>Not completed yet";

            this.setItem(slotIndex, new ItemBuilder(displayMaterial)
                    .setDisplayName(displayName)
                    .setLore(Arrays.asList(
                            "",
                            "<gray>" + achievement.getDescription(),
                            "",
                            statusLore,
                            ""
                    ))
                    .getItemStack());
        }
    }
}