package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.settings.achievements.Achievements;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommandAchievement extends CustomCommand implements CustomTabCompleter {

    public CommandAchievement() {
        super("achievements");
        setUsage("<list|grant|revoke|reset> [player] [achievement]");
        setDescription("Manage achievements");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Usage: /achievements <list|grant|revoke|reset> [player] [achievement]"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleListCommand(player, args);
            case "grant" -> handleGrantCommand(player, args);
            case "revoke" -> handleRevokeCommand(player, args);
            case "reset" -> handleResetCommand(player, args);
            default -> player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Unknown subcommand. Use: list, grant, revoke, or reset"));
        }
    }

    private void handleListCommand(Player player, String[] args) {
        UUID targetUUID;
        String targetName;

        if (args.length == 1) {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        }

        Set<String> achievements = this.plugin.getAchievementManager().getAchievementStorage().getPlayerAchievements(targetUUID);

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>========== <gold>Achievements for " + targetName + " <gray>=========="));

        if (achievements.isEmpty()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<yellow>No achievements unlocked yet!"));
        } else {
            for (String achievementName : achievements) {
                try {
                    Achievements achievement = Achievements.valueOf(achievementName);
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                            "<dark_aqua>✓ " + achievement.getTitle() + " <gray>- " + achievement.getDescription()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid achievements
                }
            }
        }

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>Total: <gold>" + achievements.size() + "<gray>/<gold>" + Achievements.values().length));
    }

    private void handleGrantCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Usage: /achievements grant <player> <achievement>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String achievementName = args[2].toUpperCase();

        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Achievement <yellow>" + achievementName + " <red>does not exist!"));
            return;
        }

        this.plugin.getAchievementManager().getAchievementStorage().addAchievement(target.getUniqueId(), achievement);

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<green>Successfully granted <yellow>" + achievement.getTitle() + " <green>to <yellow>" + target.getName()));
    }

    private void handleRevokeCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Usage: /achievements revoke <player> <achievement>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String achievementName = args[2].toUpperCase();

        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Achievement <yellow>" + achievementName + " <red>does not exist!"));
            return;
        }

        this.plugin.getAchievementManager().getAchievementStorage().removeAchievement(target.getUniqueId(), achievement);

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<green>Successfully revoked <yellow>" + achievement.getTitle() + " <green>from <yellow>" + target.getName()));
    }

    private void handleResetCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Usage: /achievements reset <player>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        this.plugin.getAchievementManager().getAchievementStorage().resetPlayerAchievements(target.getUniqueId());

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<green>Successfully reset all achievements for <yellow>" + target.getName()));
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 1) {
            return List.of("list", "grant", "revoke", "reset");
        }

        if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                playerNames.add(onlinePlayer.getName());
            }
            return playerNames;
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke"))) {
            List<String> achievementNames = new ArrayList<>();
            for (Achievements achievement : Achievements.values()) {
                achievementNames.add(achievement.name());
            }
            return achievementNames;
        }

        return null;
    }
}