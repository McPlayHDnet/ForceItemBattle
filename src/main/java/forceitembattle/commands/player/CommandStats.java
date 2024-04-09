package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.ForceItemPlayerStats;
import org.bukkit.entity.Player;

public class CommandStats extends CustomCommand {

    public CommandStats() {
        super("stats");

        setUsage("[player]");
        setDescription("Show stats");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (!this.plugin.getStatsManager().playerExists(player.getName())) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You dont have stats... I dont know why, create an issue"));
                return;
            }

            ForceItemPlayerStats forceItemPlayerStats = this.plugin.getStatsManager().playerStats(player.getName());
            this.plugin.getStatsManager().statsMessage(player, forceItemPlayerStats);
            return;
        }

        if (args.length == 1) {
            if (!this.plugin.getStatsManager().playerExists(args[0])) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[0] + " <red>does not exist"));
                return;
            }
            ForceItemPlayerStats forceItemPlayerStats = this.plugin.getStatsManager().playerStats(args[0]);
            this.plugin.getStatsManager().statsMessage(player, forceItemPlayerStats);

            return;
        }

        if (!player.isOp()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>No perms"));
            return;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("reset")) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /stats reset <username>"));
            return;
        }

        if (!this.plugin.getStatsManager().playerExists(args[1])) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + args[1] + " <red>does not exist"));
            return;
        }
        this.plugin.getStatsManager().resetStats(args[1]);
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>Successfully reset stats of <yellow>" + args[1]));
    }
}
