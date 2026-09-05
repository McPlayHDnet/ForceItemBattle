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
     * Opens whichever backpack is this player's — the team's when they are in one, their own
     * otherwise — resolved by the single rule in {@link #getBackpackForPlayer(Player)}.
     *
     * <p>Every caller that opens a backpack goes through here. {@code /bp} used to call
     * {@link #openPlayerBackpack(Player)} unconditionally, so in a team game it looked up the solo
     * map, found nothing, and handed {@code null} to {@code openInventory} — the command failed with
     * "An internal error occurred" for the whole round while the slot-8 item, which did branch,
     * worked fine.
     *
     * @return false when there is no backpack to open, which is the case when BACKPACK was switched
     *         on after the round had already started and none was ever created
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

    public void openTeamBackpack(Team team, Player player) {
        player.openInventory(this.teamBackpack.get(team));
    }
}
