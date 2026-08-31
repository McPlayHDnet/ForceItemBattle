package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.global.GlobalStat;
import forceitembattle.gui.AchievementCategoryInventory;
import forceitembattle.achievements.Achievements;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class CommandAchievement extends CustomCommand implements CustomTabCompleter {

    public CommandAchievement(ForceItemBattle plugin) {
        super(plugin, "achievements");
        setUsage("<list|grant|revoke|reset> [player] [achievement]");
        setDescription("Manage achievements");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Text.of(
                    "<red>Usage: /achievements <list|grant|revoke|reset|progress> [player] [achievement]"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleListCommand(player, args);
            case "grant" -> requireOp(player, () -> handleGrantCommand(player, args));
            case "revoke" -> requireOp(player, () -> handleRevokeCommand(player, args));
            case "reset" -> requireOp(player, () -> handleResetCommand(player, args));
            case "progress" -> handleProgressCommand(player, args);
            case "global" -> handleGlobalCommand(player, args);
            default -> player.sendMessage(Text.of(
                    "<red>Unknown subcommand. Use: list, grant, revoke, reset, or progress"));
        }
    }

    private void requireOp(Player player, Runnable action) {
        if (!requireOp(player)) return;
        action.run();
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
            new AchievementCategoryInventory(this.plugin, name, targetUUID).open(player);
        });
    }

    private void handleGlobalCommand(Player player, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length == 1) {
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            targetUuid = target.getUniqueId();
            targetName = target.getName() != null ? target.getName() : args[1];
        }

        this.plugin.getAchievementManager().getGlobalStatsLoader().load(targetUuid, stats -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendMessage(" ");
            player.sendMessage(Text.of("<dark_gray>» <gold><b>Global Stats</b> <dark_gray>● <green>" + targetName + " <dark_gray>«"));
            player.sendMessage(" ");
            for (GlobalStat stat : GlobalStat.values()) {
                player.sendMessage(Text.of("  <dark_gray>● <gray>" + stat.getLabel()
                        + " <dark_gray>» <dark_aqua>" + stats.get(stat)));
            }
            player.sendMessage(" ");
        });
    }

    private void handleGrantCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(Text.of(
                    "<red>Usage: /achievements grant <player> <achievement>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String achievementName = args[2].toUpperCase();

        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.of(
                    "<red>Achievement <yellow>" + achievementName + " <red>does not exist!"));
            return;
        }

        this.plugin.getAchievementManager().getAchievementStorage().addAchievement(target.getUniqueId(), achievement);

        player.sendMessage(Text.of(
                "<green>Successfully granted <yellow>" + achievement.getTitle() + " <green>to <yellow>" + target.getName()));
    }

    private void handleRevokeCommand(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(Text.of(
                    "<red>Usage: /achievements revoke <player> <achievement>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String achievementName = args[2].toUpperCase();

        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.of(
                    "<red>Achievement <yellow>" + achievementName + " <red>does not exist!"));
            return;
        }

        this.plugin.getAchievementManager().getAchievementStorage().removeAchievement(target.getUniqueId(), achievement);

        player.sendMessage(Text.of(
                "<green>Successfully revoked <yellow>" + achievement.getTitle() + " <green>from <yellow>" + target.getName()));
    }

    private void handleResetCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Text.of(
                    "<red>Usage: /achievements reset <player>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        this.plugin.getAchievementManager().getAchievementStorage().resetPlayerAchievements(target.getUniqueId());

        player.sendMessage(Text.of(
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
                player.sendMessage(Text.of(
                        "<red>Player must be online to inspect live progress."));
                return;
            }
            targetUUID = target.getUniqueId();
            targetName = target.getName();
            achievementArg = args[2];
        } else {
            player.sendMessage(Text.of(
                    "<red>Usage: /achievements progress [player] <achievement>"));
            return;
        }

        Achievements achievement;
        try {
            achievement = Achievements.valueOf(achievementArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.of(
                    "<red>Achievement <yellow>" + achievementArg.toUpperCase() + " <red>does not exist!"));
            return;
        }

        String progress = this.plugin.getAchievementManager().describeProgress(targetUUID, achievement);
        boolean unlocked = this.plugin.getAchievementManager()
                .getAchievementStorage().hasAchievement(targetUUID, achievement);

        player.sendMessage(Text.of(
                "<gray>===== <gold>" + achievement.getTitle() + " <gray>(" + targetName + ") ====="));
        player.sendMessage(Text.of(
                "<gray>" + achievement.getDescription()));
        player.sendMessage(Text.of(
                "<gray>Status: " + (unlocked ? "<green>Unlocked" : "<yellow>In progress")));
        player.sendMessage(Text.of(
                "<gray>Progress: <white>" + progress));
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("list", "progress", "global"));
            if (player.isOp()) {
                subs.addAll(List.of("grant", "revoke", "reset"));
            }
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            // progress self-form: /achievements progress <achievement>
            if (sub.equals("progress")) {
                return filter(achievementNames(), args[1]);
            }
            // player argument — the op-only subcommands still need one, but only for ops
            if (sub.equals("list") || sub.equals("global")
                    || (player.isOp() && (sub.equals("grant") || sub.equals("revoke") || sub.equals("reset")))) {
                return filter(onlinePlayerNames(), args[1]);
            }
            return List.of();
        }

        if (args.length == 3) {
            boolean opAction = player.isOp() && (sub.equals("grant") || sub.equals("revoke"));
            if (opAction || sub.equals("progress")) {
                return filter(achievementNames(), args[2]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String typed) {
        String prefix = typed.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private List<String> achievementNames() {
        List<String> names = new ArrayList<>();
        for (Achievements achievement : Achievements.values()) {
            names.add(achievement.name());
        }
        return names;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            names.add(onlinePlayer.getName());
        }
        return names;
    }
}
