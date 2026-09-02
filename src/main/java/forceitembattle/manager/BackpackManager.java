package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
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

        // Whether this player has a team, not whether the round was configured for them: with the
        // setting on and no team -- a spectator who joined during the countdown -- checking the
        // setting dereferences a null team. No roster entry at all is the same answer.
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
        fibPlayer.player().getInventory().setItem(8, Gamemanager.createBackpack(fibPlayer, fibPlayer.isInTeam()));
    }

    public void createTeamBackpack(Team team, ForceItemPlayer fibPlayer) {
        this.teamBackpack.put(team,
                Bukkit.createInventory(
                        null,
                        this.plugin.getConfig().getInt("settings.backpackRows") * 9,
                        Text.of("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu")));
        fibPlayer.player().getInventory().setItem(8, Gamemanager.createBackpack(fibPlayer, fibPlayer.isInTeam()));
    }

    public void openPlayerBackpack(Player player) {
        player.openInventory(this.playerBackpack.get(player.getUniqueId()));
    }

    public void openTeamBackpack(Team team, Player player) {
        player.openInventory(this.teamBackpack.get(team));
    }
}
