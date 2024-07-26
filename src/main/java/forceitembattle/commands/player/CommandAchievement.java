package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.settings.achievements.AchievementInventory;
import forceitembattle.settings.achievements.Achievements;
import forceitembattle.util.ForceItemPlayerStats;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandAchievement extends CustomCommand implements CustomTabCompleter {

    public CommandAchievement() {
        super("achievements");
        setDescription("Show achievements");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        /*
        if (args.length == 0) {
            new AchievementInventory(this.plugin, player.getName()).open(player);
            return;
        }

        if (args.length == 1) {
            String targetPlayerName = args[0];
            if (!this.plugin.getStatsManager().playerExists(targetPlayerName)) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + targetPlayerName + " <red>does not exist"));
                return;
            }
            new AchievementInventory(this.plugin, targetPlayerName).open(player);
            return;
        }

        if (!player.isOp()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>No perms"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reset" -> this.handleResetCommand(player, args);
            case "grant" -> this.handleGrantCommand(player, args);
            case "revoke" -> this.handleRevokeCommand(player, args);
        }

         */
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if(args.length != 3) {
            return null;
        }
        List<String> achievementNames = new ArrayList<>();
        for (Achievements achievement : Achievements.values()) {
            achievementNames.add(achievement.name());
        }

        return achievementNames;
    }

    /*
    private void handleResetCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /achievements reset <username>"));
            return;
        }

        String targetPlayerName = args[1];
        if (!this.plugin.getStatsManager().playerExists(targetPlayerName)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + targetPlayerName + " <red>does not exist"));
            return;
        }
        this.plugin.getStatsManager().resetAchievements(targetPlayerName);
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>Successfully reset achievements of <yellow>" + targetPlayerName));
    }

    private void handleGrantCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /achievements grant <username> <achievement>"));
            return;
        }

        String targetPlayerName = args[1];
        String achievementName = args[2].toUpperCase();
        if (!this.plugin.getStatsManager().playerExists(targetPlayerName)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + targetPlayerName + " <red>does not exist"));
            return;
        }
        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Achievement <yellow>" + achievementName + " <red>does not exist"));
            return;
        }
        ForceItemPlayerStats playerStats = this.plugin.getStatsManager().playerStats(args[1]);
        this.plugin.getAchievementManager().grantAchievement(playerStats, achievement);
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>Successfully granted <yellow>" + achievementName + " <green>to <yellow>" + targetPlayerName));
    }

    private void handleRevokeCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /achievements revoke <username> <achievement>"));
            return;
        }

        String targetPlayerName = args[1];
        String achievementName = args[2].toUpperCase();
        if (!this.plugin.getStatsManager().playerExists(targetPlayerName)) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<yellow>" + targetPlayerName + " <red>does not exist"));
            return;
        }
        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Achievement <yellow>" + achievementName + " <red>does not exist"));
            return;
        }
        ForceItemPlayerStats playerStats = this.plugin.getStatsManager().playerStats(args[1]);
        this.plugin.getAchievementManager().revokeAchievement(playerStats, achievement);
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>Successfully revoked <yellow>" + achievementName + " <green>from <yellow>" + targetPlayerName));
    }
    
     */
}
