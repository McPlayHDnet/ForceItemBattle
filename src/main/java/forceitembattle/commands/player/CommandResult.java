package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.FinishInventory;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandResult extends CustomCommand {

    public int place;

    public CommandResult() {
        super("result");
        setDescription("Show the next player's result");

        this.place = -1;
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getTimer().getTime() > 0) {
            return;
        }

        if (args.length == 1) {
            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid UUID.");
                return;
            }

            new FinishInventory(
                    this.plugin,
                    this.plugin.getGamemanager().getForceItemPlayer(uuid),
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

        Bukkit.getOnlinePlayers().forEach(players -> new FinishInventory(this.plugin, currentPlayer, currentPlace, true).open(players));

        this.place--;
    }


}
