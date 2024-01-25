package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Backpack {
    private static Map<String, Inventory> bp = new HashMap<>();

    public Backpack() {
        for (int i=1; i<=9; i++) {
            bp.put("team"+i, Bukkit.createInventory(null, ForceItemBattle.getInstance().getConfig().getInt("standard.backpackSize"), "Team " + i + " | Backpack"));
        }
    }

    public void openBackpack(Player player) {
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
