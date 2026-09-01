package forceitembattle.gui;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.AchievementScope;
import forceitembattle.achievements.Achievements;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;

public final class AchievementCategoryInventory extends InventoryBuilder {

    /** One tile per displayed scope. Scope, slot and icon travel together so they cannot drift apart. */
    private record Tile(AchievementScope scope, int slot, Material icon) {
    }

    private static final Tile[] TILES = {
            new Tile(AchievementScope.ROUND, 10, Material.CLOCK),
            new Tile(AchievementScope.GLOBAL, 12, Material.COMPASS),
            new Tile(AchievementScope.COLLECTION, 14, Material.WRITTEN_BOOK),
            new Tile(AchievementScope.META, 16, Material.NETHER_STAR),
    };

    private final ForceItemBattle plugin;
    private final String playerName;
    private final UUID playerUUID;

    public AchievementCategoryInventory(ForceItemBattle plugin, String playerName, UUID playerUUID) {
        super(9 * 3, Text.of("<dark_gray>» <dark_aqua>Achievements <dark_gray>◆ <gray>" + playerName));

        this.plugin = plugin;
        this.playerName = playerName;
        this.playerUUID = playerUUID;

        this.addUpdateHandler(this::updateInventory);
        this.addClickHandler(inventoryClickEvent -> inventoryClickEvent.setCancelled(true));
    }

    private void updateInventory() {
        this.getInventory().clear();

        this.setItems(0, 8, GuiItems.accentBorder());
        this.setItems(18, 26, GuiItems.accentBorder());

        Set<String> cachedIds = this.plugin.getAchievementManager()
                .getAchievementStorage().getPlayerAchievements(this.playerUUID);

        for (Tile tile : TILES) {
            AchievementScope scope = tile.scope();

            List<Achievements> scoped = Arrays.stream(Achievements.values())
                    .filter(achievement -> achievement.getScope() == scope)
                    .toList();
            int total = scoped.size();
            int done = (int) scoped.stream().filter(achievement -> cachedIds.contains(achievement.name())).count();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>" + scope.getSubtitle());
            lore.add("");
            lore.add("<dark_gray>» <dark_aqua>" + done + " <gray>/ <dark_aqua>" + total + " <gray>unlocked");
            lore.add("");
            lore.add("<yellow>Click to view");

            this.setItem(tile.slot(), new ItemBuilder(tile.icon())
                            .setDisplayName("<dark_gray>» <dark_aqua>" + scope.getDisplayName())
                            .setLore(lore)
                            .getItemStack(),
                    inventoryClickEvent -> {
                        this.getPlayer().playSound(this.getPlayer(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                        new AchievementInventory(this.plugin, this.playerName, this.playerUUID, scope)
                                .open(this.getPlayer());
                    });
        }
    }
}
