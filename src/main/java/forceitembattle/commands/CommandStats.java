package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayerStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStats implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandStats(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("stats").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if (strings.length == 0) {
            if (!this.forceItemBattle.getStatsManager().playerExists(player.getName())) {
                player.sendMessage("You dont have stats... I dont know why, create a issue");
                return false;
            }
            ForceItemPlayerStats forceItemPlayerStats = this.forceItemBattle.getStatsManager().playerStats(player.getName());
            this.forceItemBattle.getStatsManager().statsMessage(player, forceItemPlayerStats);
            return false;
        }

        if (strings.length == 1) {
            if (!this.forceItemBattle.getStatsManager().playerExists(strings[0])) {
                player.sendMessage("§e" + strings[0] + " §cdoes not exist");
                return false;
            }
            ForceItemPlayerStats forceItemPlayerStats = this.forceItemBattle.getStatsManager().playerStats(strings[0]);
            this.forceItemBattle.getStatsManager().statsMessage(player, forceItemPlayerStats);

            return false;
        }

        if (player.isOp()) {
            if (strings.length == 2) {
                if (strings[0].equalsIgnoreCase("reset")) {
                    if (!this.forceItemBattle.getStatsManager().playerExists(strings[1])) {
                        player.sendMessage("§e" + strings[1] + " §cdoes not exist");
                        return false;
                    }
                    this.forceItemBattle.getStatsManager().resetStats(strings[1]);
                    player.sendMessage("§aSuccessfully reset stats of §e" + strings[1]);
                } else {
                    player.sendMessage("§cUsage: /stats reset <username>");
                }
            }
        } else player.sendMessage("§cNo perms");
        return false;
    }
}
