package forceitembattle.settings.achievements;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.InventoryBuilder;
import forceitembattle.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.Arrays;
import java.util.Collections;

public class AchievementInventory extends InventoryBuilder {

    private final ForceItemBattle plugin;
    private int currentPage;

    public AchievementInventory(ForceItemBattle plugin, String playerName) {
        super(9 * 6, plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <dark_aqua>Achievements <dark_gray>● <gray>Settings"));

        this.plugin = plugin;
        this.currentPage = 0;

        this.addUpdateHandler(() -> this.updateInventory(playerName));
    }

    private int totalPages(int objectsPerPage) {
        return (int) Math.ceil((double) GameSetting.values().length / objectsPerPage);
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

    private void updateInventory(String playerName) {
        this.setItems(0, 8, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setDisplayName("<green>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setDisplayName("<green>").addItemFlags(ItemFlag.values()).getItemStack());

        ForceItemPlayerStats playerStats = ForceItemBattle.getInstance().getStatsManager().playerStats(playerName);

        int achievementSize = Achievements.values().length;
        int itemsPerPage = 36;
        int startIndex = this.currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage - 1, achievementSize - 1);

        if(achievementSize > itemsPerPage) {
            this.setItem(45, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture(this.getPageButton(27, this.currentPage, itemsPerPage))
                            .setDisplayName("<dark_red>« <red>Previous page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if(this.currentPage > 0) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage--;
                        } else this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);

                    }
            );

            this.setItem(53, new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullTexture(this.getPageButton(35, this.currentPage, itemsPerPage))
                            .setDisplayName("<dark_green>» <green>Next page")
                            .getItemStack(),
                    inventoryClickEvent -> {
                        if(this.currentPage < this.totalPages(itemsPerPage) - 1) {
                            this.getPlayer().playSound(this.getPlayer(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
                            this.currentPage++;
                        } else this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                    }
            );
        }

        for(int i = startIndex; i <= endIndex; i++) {
            int slotIndex = i - startIndex + 9;
            Achievements achievements = Achievements.values()[i];
            String settingDisplayName = "<dark_gray>» <dark_aqua>" + achievements.getTitle();
            Material completedAchievement = playerStats.achievementsDone().contains(achievements.getTitle()) ? Material.LIME_DYE : Material.GRAY_DYE;
            this.setItem(slotIndex, new ItemBuilder(completedAchievement).setDisplayName(settingDisplayName).setLore(Arrays.asList("", achievements.getDescription(), "")).getItemStack());
        }
    }
}
