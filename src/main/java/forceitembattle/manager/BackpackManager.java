package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import forceitembattle.util.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class BackpackManager implements Manager {

    private final ForceItemBattle forceItemBattle;
    private final Map<UUID, Inventory> playerBackpack;
    private final Map<Team, Inventory> teamBackpack;

    public BackpackManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.playerBackpack = new HashMap<>();
        this.teamBackpack = new HashMap<>();
    }

    public Inventory getBackpackForPlayer(Player player) {
        ForceItemPlayer forceItemPlayer = this.forceItemBattle.getRoster().get(player.getUniqueId());

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
                        this.forceItemBattle.getConfig().getInt("settings.backpackRows") * 9,
                        Text.of("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu")));
        fibPlayer.player().getInventory().setItem(8, Gamemanager.createBackpack(fibPlayer, fibPlayer.isInTeam()));
    }

    public void createTeamBackpack(Team team, ForceItemPlayer fibPlayer) {
        this.teamBackpack.put(team,
                Bukkit.createInventory(
                        null,
                        this.forceItemBattle.getConfig().getInt("settings.backpackRows") * 9,
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
