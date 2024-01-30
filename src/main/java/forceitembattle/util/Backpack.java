package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Backpack {
    private static Map<String, Inventory> bp = new HashMap<>();
    private Map<UUID, Inventory> playerBackpack;

    public Backpack() {
        this.playerBackpack = new HashMap<>();

        /* TODO teams later
        for (int i=1; i<=9; i++) {
            bp.put("team"+i, Bukkit.createInventory(null, ForceItemBattle.getInstance().getConfig().getInt("standard.backpackSize"), "Team " + i + " | Backpack"));
        }*/
    }

    public void createBackpack(Player player) {
        this.playerBackpack.put(player.getUniqueId(), Bukkit.createInventory(null, ForceItemBattle.getInstance().getConfig().getInt("standard.backpackSize"), "§8» §6Backpack §8● §7Menu"));
        player.getInventory().setItem(8, new ItemBuilder(Material.BUNDLE).setDisplayName("§8» §eBackpack").getItemStack());
    }

    public void openPlayerBackpack(Player player) {
        player.openInventory(this.playerBackpack.get(player.getUniqueId()));
    }

    public void openTeamBackpack(Player player) {
        if (ForceItemBattle.getGamemanager().getPlayerTeamSTRING(player) == null) return;
        player.openInventory(bp.get(ForceItemBattle.getGamemanager().getPlayerTeamSTRING(player)));
    }

    public void addToAllBp(ItemStack itemStack) {
        bp.forEach((key, value) -> {
            value.addItem(itemStack);
        });
    }

    public void clearAllBp() {
        bp.forEach((key, value) -> {
            value.clear();
        });
    }

    public Inventory getBp(String team) {
        return bp.get(team);
    }

    public boolean isInMap(String team) {
        return bp.containsKey(team);
    }
}
