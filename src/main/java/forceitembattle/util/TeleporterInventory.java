package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

public class TeleporterInventory extends InventoryBuilder {

    private final ForceItemBattle plugin;

    public TeleporterInventory(ForceItemBattle plugin) {
        super(9 * 6, plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <red>Teleporter <dark_gray>● <gray>Menu"));

        this.plugin = plugin;

        this.setItems(0, 8, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());
        this.setItems(45, 53, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName("<aqua>").addItemFlags(ItemFlag.values()).getItemStack());

        this.addUpdateHandler(() -> {
            int slot = 9;
            for (Player players : Bukkit.getOnlinePlayers()) {
                if(players == this.getPlayer()) continue;
                this.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("<dark_gray>» <gold>" + players.getName()).setSkullTexture(players.getPlayerProfile().getTextures()).getItemStack(), inventoryClickEvent -> {
                    Player player = (Player) inventoryClickEvent.getWhoClicked();
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_gray>[<dark_green>✔<dark_gray>] <gray>You teleported to <gold>" + players.getName()));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                });
                slot++;
            }
        });
    }
}
