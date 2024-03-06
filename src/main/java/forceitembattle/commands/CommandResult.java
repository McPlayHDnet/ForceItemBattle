package forceitembattle.commands;

import forceitembattle.settings.GameSetting;
import forceitembattle.util.FinishInventory;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.PlayerStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CommandResult extends CustomCommand {

    public int place;

    public CommandResult() {
        super("result");

        this.place = -1;
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.forceItemBattle.getTimer().getTime() > 0) {
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
                    this.forceItemBattle,
                    this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid),
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
        if (this.forceItemBattle.getGamemanager().forceItemPlayerMap().isEmpty() || this.place == 0) {
            player.sendMessage("No more players left.");
            return;
        }

        Map<UUID, ForceItemPlayer> sortedMapDesc = this.forceItemBattle.getGamemanager().sortByValue(this.forceItemBattle.getGamemanager().forceItemPlayerMap(), false);
        if (this.place == -1) {
            this.place = sortedMapDesc.size();
        }
        UUID uuid = (UUID) sortedMapDesc.keySet().toArray()[this.place - 1];

        Bukkit.getOnlinePlayers().forEach(players -> {
            new FinishInventory(this.forceItemBattle, this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid), this.place, true).open(players);
        });

        // TODO : This is not good, we should run this after timer ends automatically.
        if (forceItemBattle.getSettings().isSettingEnabled(GameSetting.STATS)) {
            ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid);
            ForceItemPlayerStats forceItemPlayerStats = forceItemBattle.getStatsManager().playerStats(forceItemPlayer.player().getName());
            forceItemBattle.getStatsManager().addToStats(PlayerStat.TRAVELLED, forceItemPlayerStats, forceItemBattle.getStatsManager().calculateDistance(forceItemPlayer.player()));

            if (forceItemPlayerStats.highestScore() < forceItemPlayer.currentScore()) {
                forceItemBattle.getStatsManager().addToStats(PlayerStat.HIGHEST_SCORE, forceItemPlayerStats, forceItemPlayer.currentScore());
            }

            if (place == 1) {
                forceItemBattle.getStatsManager().addToStats(PlayerStat.GAMES_WON, forceItemPlayerStats, 1);
            }
        }

        this.place--;
    }
}
