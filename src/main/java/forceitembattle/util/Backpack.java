package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Gamemanager;
import forceitembattle.settings.GameSetting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Backpack {

    private final ForceItemBattle forceItemBattle;
    private final Map<UUID, Inventory> playerBackpack;
    private final Map<Team, Inventory> teamBackpack;

    public Backpack(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.playerBackpack = new HashMap<>();
        this.teamBackpack = new HashMap<>();
    }

    public Inventory getBackpackForPlayer(Player player) {
        ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (forceItemBattle.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return getTeamBackpack(forceItemPlayer.currentTeam());
        }

        return getPlayerBackpack(player);
    }

    public Inventory getPlayerBackpack(Player player) {
        return this.playerBackpack.get(player.getUniqueId());
    }

    public Inventory getTeamBackpack(Team team) {
        return this.teamBackpack.get(team);
    }

    public void createBackpack(Player player) {
        this.playerBackpack.put(player.getUniqueId(),
                Bukkit.createInventory(
                        null,
                        this.forceItemBattle.getConfig().getInt("settings.backpackRows") * 9,
                        this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu")));
        player.getInventory().setItem(8, Gamemanager.createBackpack());
    }

    public void createTeamBackpack(Team team, Player player) {
        this.teamBackpack.put(team,
                Bukkit.createInventory(
                        null,
                        this.forceItemBattle.getConfig().getInt("settings.backpackRows") * 9,
                        this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu")));
        player.getInventory().setItem(8, Gamemanager.createBackpack());
    }

    public void openPlayerBackpack(Player player) {
        player.openInventory(this.playerBackpack.get(player.getUniqueId()));
    }

    public void openTeamBackpack(Team team, Player player) {
        player.openInventory(this.teamBackpack.get(team));
    }
}
