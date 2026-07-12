package forceitembattle.commands.player;

import de.threeseconds.openapi.fibservice.client.model.FibItemCountDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.util.Text;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CommandStats extends CustomCommand implements CustomTabCompleter {

    private static final long RESET_CONFIRM_TIMEOUT_MS = 30_000L;

    // Staged resets awaiting "/stats reset confirm", keyed by the admin who requested it.
    private final Map<UUID, PendingReset> pendingResets = new HashMap<>();

    private record PendingReset(String scope, UUID targetUuid, String targetName, long createdAt) {
    }

    public CommandStats(ForceItemBattle plugin) {
        super(plugin, "stats");

        setUsage("[player]");
        setDescription("Show stats");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "solo" -> handleSolo(player, args);
            case "team" -> handleTeam(player, args);
            case "duo" -> handleDuo(player, args);
            case "reset" -> handleReset(player, args);
            default -> sendUsage(player);
        }
    }

    private void handleSolo(Player player, String[] args) {
        FibStatisticsClient helper = this.plugin.getFibService().statistics();

        if (args.length == 1) {
            helper.getSoloStatisticsAsync(player.getUniqueId(),
                    stats -> sendSoloMessage(player, player.getName(), stats),
                    error -> player.sendMessage(Text.of("<red>Could not load your solo stats.")));
            return;
        }

        UUID targetUuid = resolvePlayer(args[1]);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + args[1] + " <red>was not found"));
            return;
        }

        helper.getSoloStatisticsAsync(targetUuid,
                stats -> sendSoloMessage(player, args[1], stats),
                error -> player.sendMessage(Text.of("<yellow>" + args[1] + " <red>has no solo stats yet")));
    }

    private void handleTeam(Player player, String[] args) {
        FibStatisticsClient helper = this.plugin.getFibService().statistics();

        if (args.length == 1) {
            helper.getPlayerCombinedTeamStatsAsync(player.getUniqueId(),
                    stats -> sendCombinedTeamMessage(player, player.getName(), stats),
                    error -> player.sendMessage(Text.of("<red>Could not load your team stats.")));
            return;
        }

        UUID targetUuid = resolvePlayer(args[1]);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + args[1] + " <red>was not found"));
            return;
        }

        helper.getPlayerCombinedTeamStatsAsync(targetUuid,
                stats -> sendCombinedTeamMessage(player, args[1], stats),
                error -> player.sendMessage(Text.of("<yellow>" + args[1] + " <red>has no team stats yet")));
    }

    private void handleDuo(Player player, String[] args) {
        FibStatisticsClient helper = this.plugin.getFibService().statistics();

        if (args.length < 2) {
            player.sendMessage(Text.of("<red>Usage: /stats duo <teammate> <dark_gray>or <red>/stats duo <player1> <player2>"));
            return;
        }

        UUID player1Uuid;
        UUID player2Uuid;
        String player1Name;
        String player2Name;

        if (args.length == 2) {
            player1Uuid = player.getUniqueId();
            player1Name = player.getName();

            player2Uuid = resolvePlayer(args[1]);
            player2Name = args[1];
        } else {
            player1Uuid = resolvePlayer(args[1]);
            player1Name = args[1];

            player2Uuid = resolvePlayer(args[2]);
            player2Name = args[2];
        }

        if (player1Uuid == null) {
            player.sendMessage(Text.of("<yellow>" + player1Name + " <red>was not found"));
            return;
        }
        if (player2Uuid == null) {
            player.sendMessage(Text.of("<yellow>" + player2Name + " <red>was not found"));
            return;
        }

        helper.getTeamStatisticsAsync(player1Uuid, player2Uuid,
                stats -> sendDuoMessage(player, player1Name, player2Name, stats),
                error -> player.sendMessage(Text.of("<yellow>" + player1Name + " <red>and <yellow>" + player2Name + " <red>have no duo stats yet")));
    }

    private void handleReset(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(Text.of("<red>You don't have permission to do that."));
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            confirmReset(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Text.of("<red>Usage: /stats reset <solo|team> <player>"));
            return;
        }

        String scope = args[1].toLowerCase();
        if (!scope.equals("solo") && !scope.equals("team")) {
            player.sendMessage(Text.of("<red>Usage: /stats reset <solo|team> <player>"));
            return;
        }

        String targetName = args[2];
        UUID targetUuid = resolvePlayer(targetName);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + targetName + " <red>was not found"));
            return;
        }

        this.pendingResets.put(player.getUniqueId(),
                new PendingReset(scope, targetUuid, targetName, System.currentTimeMillis()));

        player.sendMessage(Text.of("<red>You are about to reset <yellow>" + targetName + "<red>'s " + scope + " stats."));
        player.sendMessage(Text.of("<gray>This cannot be undone. Type <yellow>/stats reset confirm <gray>to proceed."));
    }

    private void confirmReset(Player player) {
        PendingReset pending = this.pendingResets.remove(player.getUniqueId());
        if (pending == null) {
            player.sendMessage(Text.of("<red>You have no pending reset to confirm."));
            return;
        }
        if (System.currentTimeMillis() - pending.createdAt() > RESET_CONFIRM_TIMEOUT_MS) {
            player.sendMessage(Text.of("<red>Your pending reset expired. Run the command again."));
            return;
        }

        FibStatisticsClient helper = this.plugin.getFibService().statistics();
        String targetName = pending.targetName();
        UUID targetUuid = pending.targetUuid();

        if (pending.scope().equals("solo")) {
            helper.deleteSoloStatisticsAsync(targetUuid,
                    () -> player.sendMessage(Text.of("<dark_aqua>Reset <green>" + targetName + "<dark_aqua>'s solo stats")),
                    error -> player.sendMessage(Text.of("<red>Could not reset <yellow>" + targetName + "<red>'s solo stats")));
        } else {
            helper.deleteAllTeamStatisticsForPlayerAsync(targetUuid,
                    () -> player.sendMessage(Text.of("<dark_aqua>Reset <green>" + targetName + "<dark_aqua>'s team stats")),
                    error -> player.sendMessage(Text.of("<red>Could not reset <yellow>" + targetName + "<red>'s team stats")));
        }
    }

    private void sendSoloMessage(Player player, String targetName, FibSoloStatisticsDto stats) {
        int gamesPlayed = stats.getGamesPlayed();
        int gamesWon = stats.getGamesWon();
        double winPct = gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>Solo Stats</b> <dark_gray>● <green>" + targetName + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(Text.of("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + stats.getTotalItemsFound()));
        sendTopItems(player, stats.getTopThreeItems());
        player.sendMessage(Text.of("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + stats.getBlocksTravelled() + " blocks"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + stats.getHighestScore()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + stats.getHighestB2BStreak()));
        sendRarities(player, stats.getRarities());
        player.sendMessage(Text.of("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + gamesPlayed));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + gamesWon));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + df.format(winPct) + "%"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Deaths <dark_gray>» <dark_aqua>" + stats.getDeaths()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Longest item streak <dark_gray>» <dark_aqua>" + stats.getLongestItemStreak()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Wheel of Fortune uses <dark_gray>» <dark_aqua>" + stats.getWheelOfFortuneUses()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Antimatter teleports <dark_gray>» <dark_aqua>" + stats.getEnteredAntimatterTeleporter()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. items / game <dark_gray>» <dark_aqua>" + df.format(gamesPlayed != 0 ? (double) stats.getTotalItemsFound() / gamesPlayed : 0)));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. back-to-backs / game <dark_gray>» <dark_aqua>" + df.format(gamesPlayed != 0 ? (double) totalRarities(stats.getRarities()) / gamesPlayed : 0)));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. time per item <dark_gray>» <dark_aqua>" + formatTime(stats.getTotalItemsFound() > 0 ? stats.getTotalTimeSpentOnItems() / stats.getTotalItemsFound() : 0)));
        player.sendMessage(" ");
    }

    private void sendCombinedTeamMessage(Player player, String targetName, FibPlayerCombinedTeamStatsDto stats) {
        int gamesPlayed = stats.getTotalGamesPlayed();
        int gamesWon = stats.getTotalGamesWon();
        double winPct = gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>Team Stats</b> <dark_gray>● <green>" + targetName + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(Text.of("  <dark_gray>● <gray>Teams played with <dark_gray>» <dark_aqua>" + stats.getTeamsCount()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + stats.getTotalItemsFound()));
        sendTopItems(player, stats.getTopThreeItems());
        player.sendMessage(Text.of("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + stats.getBlocksTravelled() + " blocks"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Highest team score <dark_gray>» <dark_aqua>" + stats.getHighestTeamScore()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + stats.getHighestB2BStreak()));
        sendRarities(player, stats.getRarities());
        player.sendMessage(Text.of("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + gamesPlayed));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + gamesWon));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + df.format(winPct) + "%"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Deaths <dark_gray>» <dark_aqua>" + stats.getDeaths()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Longest item streak <dark_gray>» <dark_aqua>" + stats.getLongestTeamItemStreak()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Wheel of Fortune uses <dark_gray>» <dark_aqua>" + stats.getWheelOfFortuneUses()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Antimatter teleports <dark_gray>» <dark_aqua>" + stats.getEnteredAntimatterTeleporter()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. items / game <dark_gray>» <dark_aqua>" + df.format(gamesPlayed != 0 ? (double) stats.getTotalItemsFound() / gamesPlayed : 0)));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. back-to-backs / game <dark_gray>» <dark_aqua>" + df.format(gamesPlayed != 0 ? (double) totalRarities(stats.getRarities()) / gamesPlayed : 0)));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. time per item <dark_gray>» <dark_aqua>" + formatTime(stats.getTotalItemsFound() > 0 ? stats.getTotalTimeSpentOnItems() / stats.getTotalItemsFound() : 0)));
        player.sendMessage(" ");
    }

    private void sendDuoMessage(Player player, String p1Name, String p2Name, FibTeamStatisticsDto stats) {
        int gamesPlayed = stats.getGamesPlayed();
        int gamesWon = stats.getGamesWon();
        double winPct = gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>Duo Stats</b> <dark_gray>● <green>" + p1Name + " <dark_gray>& <green>" + p2Name + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(Text.of("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + stats.getHighestScore()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + stats.getTotalItemsFound()));
        sendTopItems(player, stats.getTopThreeItems());
        player.sendMessage(Text.of("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + stats.getBlocksTravelled() + " blocks"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + stats.getHighestB2BStreak()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + gamesPlayed));
        sendRarities(player, stats.getRarities());
        player.sendMessage(Text.of("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + gamesWon));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + df.format(winPct) + "%"));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Deaths <dark_gray>» <dark_aqua>" + stats.getDeaths()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Longest item streak <dark_gray>» <dark_aqua>" + stats.getLongestItemStreak()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Wheel of Fortune uses <dark_gray>» <dark_aqua>" + stats.getWheelOfFortuneUses()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Antimatter teleports <dark_gray>» <dark_aqua>" + stats.getEnteredAntimatterTeleporter()));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. items / game <dark_gray>» <dark_aqua>" + df.format(gamesPlayed != 0 ? (double) stats.getTotalItemsFound() / gamesPlayed : 0)));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. back-to-backs / game <dark_gray>» <dark_aqua>" + df.format(gamesPlayed != 0 ? (double) totalRarities(stats.getRarities()) / gamesPlayed : 0)));
        player.sendMessage(Text.of("  <dark_gray>● <gray>Avg. time per item <dark_gray>» <dark_aqua>" + formatTime(stats.getTotalItemsFound() > 0 ? stats.getTotalTimeSpentOnItems() / stats.getTotalItemsFound() : 0)));
        player.sendMessage(" ");

        if (!stats.getMemberStats().isEmpty()) {
            player.sendMessage(Text.of("  <dark_gray>● <gray>Contributions <dark_gray>»"));
            for (FibTeamMemberStatsDto member : stats.getMemberStats()) {
                String memberName = resolveName(member.getMemberUuid());
                player.sendMessage(Text.of("    <dark_gray>» <green>" + memberName
                        + " <dark_gray>| <dark_aqua>" + member.getTotalItemsFound() + " items"
                        + " <dark_gray>| <dark_aqua>" + member.getDeaths() + " deaths"
                        + " <dark_gray>| <dark_aqua>" + member.getBlocksTravelled() + " blocks"));
            }
            player.sendMessage(" ");
        }
    }

    private void sendTopItems(Player player, List<FibItemCountDto> topItems) {
        if (topItems == null || topItems.isEmpty()) {
            return;
        }
        for (FibItemCountDto item : topItems) {
            Material material = Material.valueOf(item.getItemName().toUpperCase());
            String unicode = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, material);
            String formattedName = this.plugin.getGamemanager().getMaterialName(material);
            player.sendMessage(Text.of("    <dark_gray>» <reset>" + unicode + " <gray>" + formattedName + " <dark_gray>× <dark_aqua>" + item.getCount()));
        }
    }

    private void sendRarities(Player player, FibRaritiesDto rarities) {
        if (rarities == null) {
            return;
        }
        long total = totalRarities(rarities);
        if (total == 0) {
            return;
        }
        player.sendMessage(Text.of("  <dark_gray>● <gray>Rarities <dark_gray>»"));
        if (rarities.getRare() > 0)
            player.sendMessage(Text.of("    <dark_gray>» <blue>Rare <dark_gray>× <dark_aqua>" + rarities.getRare()));
        if (rarities.getEpic() > 0)
            player.sendMessage(Text.of("    <dark_gray>» <dark_purple>Epic <dark_gray>× <dark_aqua>" + rarities.getEpic()));
        if (rarities.getLegendary() > 0)
            player.sendMessage(Text.of("    <dark_gray>» <gold>Legendary <dark_gray>× <dark_aqua>" + rarities.getLegendary()));
        if (rarities.getRngesus() > 0)
            player.sendMessage(Text.of("    <dark_gray>» <gradient:#E41EBC:#9A4992>RNGesus</gradient> <dark_gray>× <dark_aqua>" + rarities.getRngesus()));
        if (rarities.getExtraordinary() > 0)
            player.sendMessage(Text.of("    <dark_gray>» <gradient:#73FF00:#14C8FF>Extraordinary</gradient> <dark_gray>× <dark_aqua>" + rarities.getExtraordinary()));
    }

    private long totalRarities(FibRaritiesDto rarities) {
        if (rarities == null) {
            return 0;
        }
        return rarities.getRare() + rarities.getEpic() + rarities.getLegendary()
                + rarities.getRngesus() + rarities.getExtraordinary();
    }

    private UUID resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null) {
            return offline.getUniqueId();
        }
        return null;
    }

    private String resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private void sendUsage(Player player) {
        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>Stats</b> <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(Text.of("  <dark_gray>● <yellow>/stats solo <dark_gray>» <gray>Your solo stats"));
        player.sendMessage(Text.of("  <dark_gray>● <yellow>/stats solo <player> <dark_gray>» <gray>Solo stats of a player"));
        player.sendMessage(Text.of("  <dark_gray>● <yellow>/stats team <dark_gray>» <gray>Your overall team stats"));
        player.sendMessage(Text.of("  <dark_gray>● <yellow>/stats team <player> <dark_gray>» <gray>Overall team stats of a player"));
        player.sendMessage(Text.of("  <dark_gray>● <yellow>/stats duo <teammate> <dark_gray>» <gray>Your stats with a teammate"));
        player.sendMessage(Text.of("  <dark_gray>● <yellow>/stats duo <p1> <p2> <dark_gray>» <gray>Duo stats between two players"));
        if (player.isOp()) {
            player.sendMessage(Text.of("  <dark_gray>● <red>/stats reset solo <player> <dark_gray>» <gray>Reset a player's solo stats"));
            player.sendMessage(Text.of("  <dark_gray>● <red>/stats reset team <player> <dark_gray>» <gray>Reset a player's team stats"));
        }
        player.sendMessage(" ");
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else if (seconds > 0) {
            return seconds + "." + (millis % 1000) / 100 + "s";
        }
        return "0." + (millis % 1000) / 100 + "s";
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("solo");
            completions.add("team");
            completions.add("duo");
            if (player.isOp()) {
                completions.add("reset");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reset")) {
                if (player.isOp()) {
                    completions.add("solo");
                    completions.add("team");
                    completions.add("confirm");
                }
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3
                && (args[0].equalsIgnoreCase("duo")
                || (args[0].equalsIgnoreCase("reset") && player.isOp()
                && (args[1].equalsIgnoreCase("solo") || args[1].equalsIgnoreCase("team"))))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }

        return completions;
    }
}
