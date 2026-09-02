package forceitembattle.gui;

import forceitembattle.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class TeleporterInventory extends InventoryBuilder {


    public TeleporterInventory() {
        super(9 * 6, Text.of("<dark_gray>» <red>Teleporter <dark_gray>● <gray>Menu"));


        this.setItems(0, 8, GuiItems.border());
        this.setItems(45, 53, GuiItems.border());

        this.addUpdateHandler(() -> {
            int slot = 9;
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players == this.getPlayer()) continue;
                this.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("<dark_gray>» <gold>" + players.getName()).setSkullTexture(players.getPlayerProfile().getTextures()).getItemStack(), inventoryClickEvent -> {
                    Player player = (Player) inventoryClickEvent.getWhoClicked();
                    player.sendMessage(Text.of("<dark_gray>[<dark_green>✔<dark_gray>] <gray>You teleported to <gold>" + players.getName()));
                    player.teleport(players.getLocation());
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                });
                slot++;
            }
        });
    }
}
