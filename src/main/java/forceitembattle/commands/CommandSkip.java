package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSkip implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!ForceItemBattle.getTimer().isRunning()) {
            sender.sendMessage(ChatColor.RED + "The game is not running. Start it first with /start");
            return false;
        } else
        if (args.length == 1) {
            if (Bukkit.getPlayer(args[0]) != null) {
                if (ForceItemBattle.getInstance().getConfig().getBoolean("settings.isTeamGame")) {
                    /////////////////////////////////////// TEAMS ///////////////////////////////////////
                    ForceItemBattle.getInstance().logToFile("[" + ForceItemBattle.getTimer().getTime() + "] | " + args[0] + " skipped " + ForceItemBattle.getGamemanager().getMaterialTeamsFromPlayer(Bukkit.getPlayer(args[0])));
                } else {
                    player.sendMessage("§7You skipped this item");
                    ForceItemBattle.getInstance().logToFile("[" + ForceItemBattle.getTimer().getTime() + "] | " + args[0] + " skipped " + ForceItemBattle.getGamemanager().getCurrentMaterial(Bukkit.getPlayer(args[0])));
                }
                ForceItemBattle.getGamemanager().skipItem(args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "This player is not online");
            }
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /skip <player_name>");
            return false;
        }
    }
}
