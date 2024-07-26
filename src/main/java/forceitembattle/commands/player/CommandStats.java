package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.manager.stats.StatsManager;
import forceitembattle.settings.achievements.Achievements;
import forceitembattle.util.ForceItemPlayerStats;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandStats extends CustomCommand implements CustomTabCompleter {

    public CommandStats() {
        super("stats");

        setUsage("[player]");
        setDescription("Show stats");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        StatsManager statsManager = this.plugin.getStatsManager();
        MiniMessage miniMessage = this.plugin.getGamemanager().getMiniMessage();

        if (args.length == 0) {
            ForceItemPlayerStats forceItemPlayerStats = statsManager.loadPlayerStats(player.getName());
            statsManager.statsMessage(player, forceItemPlayerStats, StatsManager.CURRENT_SEASON);
            return;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("teams")) {
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize("<red>Usage: /stats teams <username>"));
                    return;
                }

                if (!statsManager.statsExist(args[1])) {
                    player.sendMessage(miniMessage.deserialize("<yellow>" + args[1] + " <red>does not exist"));
                    return;
                }

                ForceItemPlayerStats forceItemPlayerStats = statsManager.loadPlayerStats(args[1]);
                player.sendMessage(miniMessage.deserialize("<red>Team-Stats are currently WIP"));
                // todo:
                // statsManager.teamStatsMessage(player, forceItemPlayerStats, StatsManager.CURRENT_SEASON);
                return;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize("<red>Usage: /stats reset <username>"));
                    return;
                }

                if (!statsManager.statsExist(args[1])) {
                    player.sendMessage(miniMessage.deserialize("<yellow>" + args[1] + " <red>does not exist"));
                    return;
                }

                ForceItemPlayerStats forceItemPlayerStats = statsManager.loadPlayerStats(args[1]);
                statsManager.resetStats(forceItemPlayerStats, StatsManager.CURRENT_SEASON);
                player.sendMessage(miniMessage.deserialize("<gray>Successfully reset stats of <yellow>" + args[1]));
                return;
            }

            if (!statsManager.statsExist(args[0])) {
                player.sendMessage(miniMessage.deserialize("<yellow>" + args[0] + " <red>does not exist"));
                return;
            }

            if (args.length == 1) {
                ForceItemPlayerStats forceItemPlayerStats = statsManager.loadPlayerStats(args[0]);
                statsManager.statsMessage(player, forceItemPlayerStats, StatsManager.CURRENT_SEASON);
                return;
            }

            if (args.length == 2) {
                String season = args[1];
                if (!statsManager.hasSeason(args[0], season)) {
                    player.sendMessage(miniMessage.deserialize("<yellow>" + args[0] + " <red>does not have stats for season " + season));
                    return;
                }
                ForceItemPlayerStats forceItemPlayerStats = statsManager.loadPlayerStats(args[0]);
                statsManager.statsMessage(player, forceItemPlayerStats, season);
                return;
            }
        }

        if (!player.isOp()) {
            player.sendMessage(miniMessage.deserialize("<red>No perms"));
            return;
        }

        if (!statsManager.statsExist(args[1])) {
            player.sendMessage(miniMessage.deserialize("<yellow>" + args[1] + " <red>does not exist"));
            return;
        }

        player.sendMessage(miniMessage.deserialize("<red>Usage: /stats reset <username>"));
        return;

    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
            }
        } else if (args.length == 2 && ForceItemBattle.getInstance().getStatsManager().statsExist(args[0])) {
            ForceItemPlayerStats stats = ForceItemBattle.getInstance().getStatsManager().loadPlayerStats(args[0]);
            completions.addAll(stats.getSeasons());
        }
        return completions;
    }
}
