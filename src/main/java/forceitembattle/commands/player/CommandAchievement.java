package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.achievements.AchievementInventory;
import forceitembattle.achievements.Achievements;
import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandAchievement extends CustomCommand implements CustomTabCompleter {

    public CommandAchievement(ForceItemBattle plugin) {
        super(plugin, "achievements");
        setUsage("<list|grant|revoke|reset> [player] [achievement]");
        setDescription("Manage achievements");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Usage: /achievements <list|grant|revoke|reset|progress> [player] [achievement]"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleListCommand(player, args);
            case "grant" -> handleGrantCommand(player, args);
            case "revoke" -> handleRevokeCommand(player, args);
            case "reset" -> handleResetCommand(player, args);
            case "progress" -> handleProgressCommand(player, args);
            default -> player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Unknown subcommand. Use: list, grant, revoke, reset, or progress"));
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
            targetName = target.getName() != null ? target.getName() : args[1];
        }

        final String name = targetName;

        this.plugin.getAchievementManager().getAchievementStorage().loadPlayer(targetUUID, () -> {
            if (!player.isOnline()) {
                return;
            }
            new AchievementInventory(this.plugin, name, targetUUID).open(player);
        });
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

    private void handleProgressCommand(Player player, String[] args) {
        UUID targetUUID;
        String targetName;
        String achievementArg;

        if (args.length == 2) {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
            achievementArg = args[1];
        } else if (args.length == 3) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                        "<red>Player must be online to inspect live progress."));
                return;
            }
            targetUUID = target.getUniqueId();
            targetName = target.getName();
            achievementArg = args[2];
        } else {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Usage: /achievements progress [player] <achievement>"));
            return;
        }

        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                    "<red>Achievement <yellow>" + achievementArg.toUpperCase() + " <red>does not exist!"));
            return;
        }

        String progress = this.plugin.getAchievementManager().describeProgress(targetUUID, achievement);
        boolean unlocked = this.plugin.getAchievementManager()
                .getAchievementStorage().hasAchievement(targetUUID, achievement);

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>===== <gold>" + achievement.getTitle() + " <gray>(" + targetName + ") ====="));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>" + achievement.getDescription()));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>Status: " + (unlocked ? "<green>Unlocked" : "<yellow>In progress")));
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>Progress: <white>" + progress));
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 1) {
            return List.of("list", "grant", "revoke", "reset", "progress");
        }

        if (args.length == 2) {
            // progress self-form: /achievements progress <achievement>
            if (args[0].equalsIgnoreCase("progress")) {
                List<String> achievementNames = new ArrayList<>();
                for (Achievements achievement : Achievements.values()) {
                    achievementNames.add(achievement.name());
                }
                return achievementNames;
            }
            List<String> playerNames = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                playerNames.add(onlinePlayer.getName());
            }
            return playerNames;
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("grant")
                || args[0].equalsIgnoreCase("revoke")
                || args[0].equalsIgnoreCase("progress"))) {
            List<String> achievementNames = new ArrayList<>();
            for (Achievements achievement : Achievements.values()) {
                achievementNames.add(achievement.name());
            }
            return achievementNames;
        }

        return null;
    }
}