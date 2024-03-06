package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.ForceItemPlayerStats;
import org.bukkit.entity.Player;

public class CommandStats extends CustomCommand {

    public CommandStats() {
        super("stats");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (!this.plugin.getStatsManager().playerExists(player.getName())) {
                player.sendMessage("§cYou dont have stats... I dont know why, create an issue");
                return;
            }

            ForceItemPlayerStats forceItemPlayerStats = this.plugin.getStatsManager().playerStats(player.getName());
            this.plugin.getStatsManager().statsMessage(player, forceItemPlayerStats);
            return;
        }

        if (args.length == 1) {
            if (!this.plugin.getStatsManager().playerExists(args[0])) {
                player.sendMessage("§e" + args[0] + " §cdoes not exist");
                return;
            }
            ForceItemPlayerStats forceItemPlayerStats = this.plugin.getStatsManager().playerStats(args[0]);
            this.plugin.getStatsManager().statsMessage(player, forceItemPlayerStats);

            return;
        }

        if (!player.isOp()) {
            player.sendMessage("§cNo perms");
            return;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("reset")) {
            player.sendMessage("§cUsage: /stats reset <username>");
            return;
        }

        if (!this.plugin.getStatsManager().playerExists(args[1])) {
            player.sendMessage("§e" + args[1] + " §cdoes not exist");
            return;
        }
        this.plugin.getStatsManager().resetStats(args[1]);
        player.sendMessage("§aSuccessfully reset stats of §e" + args[1]);
    }
}
