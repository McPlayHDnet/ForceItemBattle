package forceitembattle.manager;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.GameItems;
import forceitembattle.model.Roster;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class BackpackManager implements Manager {

    private final GameSettings settings;
    private final Roster roster;
    private final Map<UUID, Inventory> playerBackpack;
    private final Map<Team, Inventory> teamBackpack;

    public BackpackManager(GameSettings settings, Roster roster) {
        this.settings = settings;
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
        this.playerBackpack.computeIfAbsent(fibPlayer.player().getUniqueId(), uuid -> this.newBackpack());
        fibPlayer.player().getInventory().setItem(8, GameItems.backpack(fibPlayer));
    }

    public void createTeamBackpack(Team team, ForceItemPlayer fibPlayer) {
        // Once per team: a member set up late, e.g. after rejoining, must not empty the shared one.
        this.teamBackpack.computeIfAbsent(team, key -> this.newBackpack());
        fibPlayer.player().getInventory().setItem(8, GameItems.backpack(fibPlayer));
    }

    /** At round start, so a second round in the same session starts with empty backpacks. */
    public void clear() {
        this.playerBackpack.clear();
        this.teamBackpack.clear();
    }

    private Inventory newBackpack() {
        return Bukkit.createInventory(null, this.settings.backpackRows() * 9,
                Text.of("<dark_gray>» <gold>Backpack <dark_gray>● <gray>Menu"));
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
}
