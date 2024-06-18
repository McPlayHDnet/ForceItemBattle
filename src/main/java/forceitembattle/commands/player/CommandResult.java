package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.FinishInventory;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CommandResult extends CustomCommand {

    public int place;

    public CommandResult() {
        super("result");
        setDescription("Show the next player's result");

        this.place = -1;
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getTimer().getTimeLeft() > 0) {
            return;
        }

        if (args.length == 1) {
            UUID uuid = null;
            Team team = null;
            if(!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
                try {
                    uuid = UUID.fromString(args[0]);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Invalid UUID."));
                    return;
                }
            } else {
                try {
                    team = this.plugin.getTeamManager().getTeams().get(Integer.parseInt(args[0].replace("#", "")) - 1);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Invalid team."));
                }
            }

            new FinishInventory(
                    this.plugin,
                    this.plugin.getGamemanager().getForceItemPlayer(uuid),
                    team,
                    null,
                    false
            ).open(player);
            return;
        }

        if (args.length == 0 && player.isOp()) {
            showNextPlayer(player);
        }
    }

    private void showNextPlayer(Player player) {
        if(!this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            if (this.plugin.getGamemanager().forceItemPlayerMap().isEmpty() || this.place == 0) {
                player.sendMessage("No more players left.");
                return;
            }

            Map<UUID, ForceItemPlayer> sortedMapDesc = this.plugin.getGamemanager().sortByValue(this.plugin.getGamemanager().forceItemPlayerMap(), false);
            if (this.place == -1) {
                this.place = sortedMapDesc.size();
            }

            Map<ForceItemPlayer, Integer> placesMap = this.plugin.getGamemanager().calculatePlaces(sortedMapDesc);

            ForceItemPlayer currentPlayer = sortedMapDesc.values().toArray(new ForceItemPlayer[0])[this.place - 1];
            int currentPlace = placesMap.get(currentPlayer);

            Bukkit.getOnlinePlayers().forEach(players -> new FinishInventory(this.plugin, currentPlayer, null, currentPlace, true).open(players));

            this.place--;

        } else {
            if (this.plugin.getGamemanager().forceItemPlayerMap().isEmpty() || this.place == 0) {
                player.sendMessage("No more teams left.");
                return;
            }

            Map<Team, Integer> placesMap = this.plugin.getGamemanager().calculatePlaces(this.plugin.getTeamManager().getTeams());
            if (this.place == -1) {
                this.place = placesMap.size();
            }

            Team currentTeam = placesMap.keySet().toArray(new Team[0])[this.place - 1];
            int currentPlace = placesMap.get(currentTeam);

            Bukkit.getOnlinePlayers().forEach(players -> new FinishInventory(this.plugin, null, currentTeam, currentPlace, true).open(players));

            this.place--;

        }

    }


}
