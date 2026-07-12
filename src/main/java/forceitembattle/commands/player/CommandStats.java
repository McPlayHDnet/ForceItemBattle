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
import forceitembattle.model.Rarity;
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
import org.jetbrains.annotations.Nullable;

public class CommandStats extends CustomCommand implements CustomTabCompleter {

    private static final long RESET_CONFIRM_TIMEOUT_MS = 30_000L;

    // Staged resets awaiting "/stats reset confirm", keyed by the admin who requested it.
    private final Map<UUID, PendingReset> pendingResets = new HashMap<>();

    private record PendingReset(String scope, UUID targetUuid, String targetName, long createdAt) {
    }

    private record StatsView(
            long gamesPlayed,
            long gamesWon,
            long totalItemsFound,
            List<FibItemCountDto> topThreeItems,
            long blocksTravelled,
            long highestScore,
            String scoreLabel,
            long highestB2BStreak,
            FibRaritiesDto rarities,
            long deaths,
            long longestItemStreak,
            long wheelOfFortuneUses,
            long antimatterTeleports,
            long totalTimeSpentOnItems,
            @Nullable Long teamsPlayedWith
    ) {

        static StatsView of(FibSoloStatisticsDto stats) {
            return new StatsView(
                    stats.getGamesPlayed(), stats.getGamesWon(),
                    stats.getTotalItemsFound(), stats.getTopThreeItems(),
                    stats.getBlocksTravelled(),
                    stats.getHighestScore(), "Highest score",
                    stats.getHighestB2BStreak(), stats.getRarities(),
                    stats.getDeaths(), stats.getLongestItemStreak(),
                    stats.getWheelOfFortuneUses(), stats.getEnteredAntimatterTeleporter(),
                    stats.getTotalTimeSpentOnItems(),
                    null
            );
        }

        static StatsView of(FibPlayerCombinedTeamStatsDto stats) {
            return new StatsView(
                    stats.getTotalGamesPlayed(), stats.getTotalGamesWon(),
                    stats.getTotalItemsFound(), stats.getTopThreeItems(),
                    stats.getBlocksTravelled(),
                    stats.getHighestTeamScore(), "Highest team score",
                    stats.getHighestB2BStreak(), stats.getRarities(),
                    stats.getDeaths(), stats.getLongestTeamItemStreak(),
                    stats.getWheelOfFortuneUses(), stats.getEnteredAntimatterTeleporter(),
                    stats.getTotalTimeSpentOnItems(),
                    (long) stats.getTeamsCount()
            );
        }

        static StatsView of(FibTeamStatisticsDto stats) {
            return new StatsView(
                    stats.getGamesPlayed(), stats.getGamesWon(),
                    stats.getTotalItemsFound(), stats.getTopThreeItems(),
                    stats.getBlocksTravelled(),
                    stats.getHighestScore(), "Highest score",
                    stats.getHighestB2BStreak(), stats.getRarities(),
                    stats.getDeaths(), stats.getLongestItemStreak(),
                    stats.getWheelOfFortuneUses(), stats.getEnteredAntimatterTeleporter(),
                    stats.getTotalTimeSpentOnItems(),
                    null
            );
        }

        double winPercentage() {
            return gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        }

        double averageItemsPerGame() {
            return gamesPlayed != 0 ? (double) totalItemsFound / gamesPlayed : 0;
        }

        double averageBackToBacksPerGame() {
            return gamesPlayed != 0 ? (double) Rarity.total(rarities) / gamesPlayed : 0;
        }

        long averageTimePerItem() {
            return totalItemsFound > 0 ? totalTimeSpentOnItems / totalItemsFound : 0;
        }
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
                    stats -> sendStats(player, "Solo Stats", "<green>" + player.getName(), StatsView.of(stats)),
                    error -> player.sendMessage(Text.of("<red>Could not load your solo stats.")));
            return;
        }

        UUID targetUuid = resolvePlayer(args[1]);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + args[1] + " <red>was not found"));
            return;
        }

        helper.getSoloStatisticsAsync(targetUuid,
                stats -> sendStats(player, "Solo Stats", "<green>" + args[1], StatsView.of(stats)),
                error -> player.sendMessage(Text.of("<yellow>" + args[1] + " <red>has no solo stats yet")));
    }

    private void handleTeam(Player player, String[] args) {
        FibStatisticsClient helper = this.plugin.getFibService().statistics();

        if (args.length == 1) {
            helper.getPlayerCombinedTeamStatsAsync(player.getUniqueId(),
                    stats -> sendStats(player, "Team Stats", "<green>" + player.getName(), StatsView.of(stats)),
                    error -> player.sendMessage(Text.of("<red>Could not load your team stats.")));
            return;
        }

        UUID targetUuid = resolvePlayer(args[1]);
        if (targetUuid == null) {
            player.sendMessage(Text.of("<yellow>" + args[1] + " <red>was not found"));
            return;
        }

        helper.getPlayerCombinedTeamStatsAsync(targetUuid,
                stats -> sendStats(player, "Team Stats", "<green>" + args[1], StatsView.of(stats)),
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

        String subject = "<green>" + player1Name + " <dark_gray>& <green>" + player2Name;

        helper.getTeamStatisticsAsync(player1Uuid, player2Uuid,
                stats -> {
                    sendStats(player, "Duo Stats", subject, StatsView.of(stats));
                    sendContributions(player, stats.getMemberStats());
                },
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

    /**
     * The stats screen. Solo, team and duo all render through here — only the header,
     * the score label and the optional "teams played with" line differ.
     */
    private void sendStats(Player player, String title, String subject, StatsView view) {
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>" + title + "</b> <dark_gray>● " + subject + " <dark_gray>«"));
        player.sendMessage(" ");

        if (view.teamsPlayedWith() != null) {
            sendLine(player, "Teams played with", view.teamsPlayedWith());
        }

        sendLine(player, "Total items found", view.totalItemsFound());
        sendTopItems(player, view.topThreeItems());
        sendLine(player, "Travelled", view.blocksTravelled() + " blocks");
        sendLine(player, view.scoreLabel(), view.highestScore());
        sendLine(player, "Back-to-Back streak", view.highestB2BStreak());
        sendRarities(player, view.rarities());
        sendLine(player, "Games played", view.gamesPlayed());
        sendLine(player, "Games won", view.gamesWon());
        sendLine(player, "Win percentage", df.format(view.winPercentage()) + "%");
        sendLine(player, "Deaths", view.deaths());
        sendLine(player, "Longest item streak", view.longestItemStreak());
        sendLine(player, "Wheel of Fortune uses", view.wheelOfFortuneUses());
        sendLine(player, "Antimatter teleports", view.antimatterTeleports());
        sendLine(player, "Avg. items / game", df.format(view.averageItemsPerGame()));
        sendLine(player, "Avg. back-to-backs / game", df.format(view.averageBackToBacksPerGame()));
        sendLine(player, "Avg. time per item", formatTime(view.averageTimePerItem()));
        player.sendMessage(" ");
    }

    private void sendLine(Player player, String label, Object value) {
        player.sendMessage(Text.of("  <dark_gray>● <gray>" + label + " <dark_gray>» <dark_aqua>" + value));
    }

    private void sendContributions(Player player, List<FibTeamMemberStatsDto> memberStats) {
        if (memberStats == null || memberStats.isEmpty()) {
            return;
        }

        player.sendMessage(Text.of("  <dark_gray>● <gray>Contributions <dark_gray>»"));
        for (FibTeamMemberStatsDto member : memberStats) {
            String memberName = resolveName(member.getMemberUuid());
            player.sendMessage(Text.of("    <dark_gray>» <green>" + memberName
                    + " <dark_gray>| <dark_aqua>" + member.getTotalItemsFound() + " items"
                    + " <dark_gray>| <dark_aqua>" + member.getDeaths() + " deaths"
                    + " <dark_gray>| <dark_aqua>" + member.getBlocksTravelled() + " blocks"));
        }
        player.sendMessage(" ");
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
        if (Rarity.total(rarities) == 0) {
            return;
        }

        player.sendMessage(Text.of("  <dark_gray>● <gray>Rarities <dark_gray>»"));
        for (Rarity rarity : Rarity.values()) {
            long count = rarity.count(rarities);
            if (count > 0) {
                player.sendMessage(Text.of("    <dark_gray>» " + rarity.displayName() + " <dark_gray>× <dark_aqua>" + count));
            }
        }
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
