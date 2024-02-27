package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandStats implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandStats(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("stats").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if(strings.length == 0) {
            if(!this.forceItemBattle.getStatsManager().playerExists(player.getName())) {
                player.sendMessage("You dont have stats... I dont know why, create a issue");
                return false;
            }
            ForceItemPlayerStats forceItemPlayerStats = this.forceItemBattle.getStatsManager().playerStats(player.getName());
            this.forceItemBattle.getStatsManager().statsMessage(player, forceItemPlayerStats);
            return false;
        }

        if(strings.length == 1) {
            if(!this.forceItemBattle.getStatsManager().playerExists(strings[0])) {
                player.sendMessage("§e" + strings[0] + " §cdoes not exist");
                return false;
            }
            ForceItemPlayerStats forceItemPlayerStats = this.forceItemBattle.getStatsManager().playerStats(strings[0]);
            this.forceItemBattle.getStatsManager().statsMessage(player, forceItemPlayerStats);
        }
        return false;
    }
}
