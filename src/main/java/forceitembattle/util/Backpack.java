package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Backpack {

    private final ForceItemBattle forceItemBattle;
    private final Map<UUID, Inventory> playerBackpack;

    public Backpack(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.playerBackpack = new HashMap<>();
    }

    public Inventory getPlayerBackpack(Player player) {
        return this.playerBackpack.get(player.getUniqueId());
    }

    public void createBackpack(Player player) {
        this.playerBackpack.put(player.getUniqueId(), Bukkit.createInventory(null, this.forceItemBattle.getConfig().getInt("standard.backpackSize"), "§8» §6Backpack §8● §7Menu"));
        player.getInventory().setItem(8, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» §eBackpack").getItemStack());
    }

    public void openPlayerBackpack(Player player) {
        player.openInventory(this.playerBackpack.get(player.getUniqueId()));
    }
}
