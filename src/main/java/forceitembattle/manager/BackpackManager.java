package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameItems;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import forceitembattle.util.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class BackpackManager implements Manager {

    private final JavaPlugin plugin;
    private final Roster roster;
    private final Map<UUID, Inventory> playerBackpack;
    private final Map<Team, Inventory> teamBackpack;

    public BackpackManager(JavaPlugin plugin, Roster roster) {
        this.plugin = plugin;
        this.roster = roster;
        this.playerBackpack = new HashMap<>();
        this.teamBackpack = new HashMap<>();
    }

    public Inventory getBackpackForPlayer(Player player) {
        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());

        // Whether this player has a team, not the setting: a spectator who joined during the countdown has none.
        if (forceItemPlayer != null && forceItemPlayer.isInTeam()) {
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

    public void createBackpack(ForceItemPlayer fibPlayer) {
        this.playerBackpack.put(fibPlayer.player().getUniqueId(),
                Bukkit.createInventory(
                        null,
                        this.plugin.getConfig().getInt("settings.backpackRows") * 9,
                        Text.of("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu")));
        fibPlayer.player().getInventory().setItem(8, GameItems.backpack(fibPlayer));
    }

    public void createTeamBackpack(Team team, ForceItemPlayer fibPlayer) {
        this.teamBackpack.put(team,
                Bukkit.createInventory(
                        null,
                        this.plugin.getConfig().getInt("settings.backpackRows") * 9,
                        Text.of("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu")));
        fibPlayer.player().getInventory().setItem(8, GameItems.backpack(fibPlayer));
    }

    /**
     * Every caller that opens a backpack goes through here, so solo and team resolve the same way.
     *
     * @return false when BACKPACK was switched on mid-round and none was ever created
     */
    public boolean openBackpackFor(Player player) {
        Inventory backpack = getBackpackForPlayer(player);
        if (backpack == null) {
            return false;
        }
        player.openInventory(backpack);
        return true;
    }

    public void openPlayerBackpack(Player player) {
        player.openInventory(this.playerBackpack.get(player.getUniqueId()));
    }
}
