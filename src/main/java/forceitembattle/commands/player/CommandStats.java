package forceitembattle.commands.player;

import de.threeseconds.openapi.fibservice.client.model.FibItemCountDto;
import de.threeseconds.openapi.fibservice.client.model.FibPlayerCombinedTeamStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibRaritiesDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamMemberStatsDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamStatisticsDto;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.stats.FIBServiceHelper;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandStats extends CustomCommand implements CustomTabCompleter {

    public CommandStats() {
        super("stats");

        setUsage("[player]");
        setDescription("Show stats");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        MiniMessage mm = this.plugin.getGamemanager().getMiniMessage();

        if (args.length == 0) {
            sendUsage(player, mm);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "solo" -> handleSolo(player, args, mm);
            case "team" -> handleTeam(player, args, mm);
            case "duo" -> handleDuo(player, args, mm);
            default -> sendUsage(player, mm);
        }
    }

    private void handleSolo(Player player, String[] args, MiniMessage mm) {
        FIBServiceHelper helper = this.plugin.getFibServiceHelper();

        if (args.length == 1) {
            helper.getSoloStatisticsAsync(player.getUniqueId(),
                    stats -> sendSoloMessage(player, player.getName(), stats, mm),
                    error -> player.sendMessage(mm.deserialize("<red>Could not load your solo stats.")));
            return;
        }

        UUID targetUuid = resolvePlayer(args[1]);
        if (targetUuid == null) {
            player.sendMessage(mm.deserialize("<yellow>" + args[1] + " <red>was not found"));
            return;
        }

        helper.getSoloStatisticsAsync(targetUuid,
                stats -> sendSoloMessage(player, args[1], stats, mm),
                error -> player.sendMessage(mm.deserialize("<yellow>" + args[1] + " <red>has no solo stats yet")));
    }

    private void handleTeam(Player player, String[] args, MiniMessage mm) {
        FIBServiceHelper helper = this.plugin.getFibServiceHelper();

        if (args.length == 1) {
            helper.getPlayerCombinedTeamStatsAsync(player.getUniqueId(),
                    stats -> sendCombinedTeamMessage(player, player.getName(), stats, mm),
                    error -> player.sendMessage(mm.deserialize("<red>Could not load your team stats.")));
            return;
        }

        UUID targetUuid = resolvePlayer(args[1]);
        if (targetUuid == null) {
            player.sendMessage(mm.deserialize("<yellow>" + args[1] + " <red>was not found"));
            return;
        }

        helper.getPlayerCombinedTeamStatsAsync(targetUuid,
                stats -> sendCombinedTeamMessage(player, args[1], stats, mm),
                error -> player.sendMessage(mm.deserialize("<yellow>" + args[1] + " <red>has no team stats yet")));
    }

    private void handleDuo(Player player, String[] args, MiniMessage mm) {
        FIBServiceHelper helper = this.plugin.getFibServiceHelper();

        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /stats duo <teammate> <dark_gray>or <red>/stats duo <player1> <player2>"));
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
            player.sendMessage(mm.deserialize("<yellow>" + player1Name + " <red>was not found"));
            return;
        }
        if (player2Uuid == null) {
            player.sendMessage(mm.deserialize("<yellow>" + player2Name + " <red>was not found"));
            return;
        }

        helper.getTeamStatisticsAsync(player1Uuid, player2Uuid,
                stats -> sendDuoMessage(player, player1Name, player2Name, stats, mm),
                error -> player.sendMessage(mm.deserialize("<yellow>" + player1Name + " <red>and <yellow>" + player2Name + " <red>have no duo stats yet")));
    }

    private void sendSoloMessage(Player player, String targetName, FibSoloStatisticsDto stats, MiniMessage mm) {
        int gamesPlayed = stats.getGamesPlayed();
        int gamesWon = stats.getGamesWon();
        double winPct = gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("<dark_gray>» <gold><b>Solo Stats</b> <dark_gray>● <green>" + targetName + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + stats.getTotalItemsFound()));
        sendTopItems(player, stats.getTopThreeItems(), mm);
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + stats.getBlocksTravelled() + " blocks"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + stats.getHighestScore()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + stats.getHighestB2BStreak()));
        sendRarities(player, stats.getRarities(), mm);
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + gamesPlayed));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + gamesWon));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + df.format(winPct) + "%"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Deaths <dark_gray>» <dark_aqua>" + stats.getDeaths()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Longest item streak <dark_gray>» <dark_aqua>" + stats.getLongestItemStreak()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Wheel of Fortune uses <dark_gray>» <dark_aqua>" + stats.getWheelOfFortuneUses()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Antimatter teleports <dark_gray>» <dark_aqua>" + stats.getEnteredAntimatterTeleporter()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Avg. time per item <dark_gray>» <dark_aqua>" + formatTime(stats.getTotalItemsFound() > 0 ? stats.getTotalTimeSpentOnItems() / stats.getTotalItemsFound() : 0)));
        player.sendMessage(" ");
    }

    private void sendCombinedTeamMessage(Player player, String targetName, FibPlayerCombinedTeamStatsDto stats, MiniMessage mm) {
        int gamesPlayed = stats.getTotalGamesPlayed();
        int gamesWon = stats.getTotalGamesWon();
        double winPct = gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("<dark_gray>» <gold><b>Team Stats</b> <dark_gray>● <green>" + targetName + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Teams played with <dark_gray>» <dark_aqua>" + stats.getTeamsCount()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + stats.getTotalItemsFound()));
        sendTopItems(player, stats.getTopThreeItems(), mm);
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + stats.getBlocksTravelled() + " blocks"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Highest team score <dark_gray>» <dark_aqua>" + stats.getHighestTeamScore()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + stats.getHighestB2BStreak()));
        sendRarities(player, stats.getRarities(), mm);
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + gamesPlayed));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + gamesWon));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + df.format(winPct) + "%"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Deaths <dark_gray>» <dark_aqua>" + stats.getDeaths()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Longest item streak <dark_gray>» <dark_aqua>" + stats.getLongestTeamItemStreak()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Wheel of Fortune uses <dark_gray>» <dark_aqua>" + stats.getWheelOfFortuneUses()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Antimatter teleports <dark_gray>» <dark_aqua>" + stats.getEnteredAntimatterTeleporter()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Avg. time per item <dark_gray>» <dark_aqua>" + formatTime(stats.getTotalItemsFound() > 0 ? stats.getTotalTimeSpentOnItems() / stats.getTotalItemsFound() : 0)));
        player.sendMessage(" ");
    }

    private void sendDuoMessage(Player player, String p1Name, String p2Name, FibTeamStatisticsDto stats, MiniMessage mm) {
        int gamesPlayed = stats.getGamesPlayed();
        int gamesWon = stats.getGamesWon();
        double winPct = gamesPlayed != 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        DecimalFormat df = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("<dark_gray>» <gold><b>Duo Stats</b> <dark_gray>● <green>" + p1Name + " <dark_gray>& <green>" + p2Name + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + stats.getHighestScore()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + stats.getTotalItemsFound()));
        sendTopItems(player, stats.getTopThreeItems(), mm);
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + stats.getBlocksTravelled() + " blocks"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + stats.getHighestB2BStreak()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + gamesPlayed));
        sendRarities(player, stats.getRarities(), mm);
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + gamesWon));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + df.format(winPct) + "%"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Deaths <dark_gray>» <dark_aqua>" + stats.getDeaths()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Longest item streak <dark_gray>» <dark_aqua>" + stats.getLongestItemStreak()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Wheel of Fortune uses <dark_gray>» <dark_aqua>" + stats.getWheelOfFortuneUses()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Antimatter teleports <dark_gray>» <dark_aqua>" + stats.getEnteredAntimatterTeleporter()));
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Avg. time per item <dark_gray>» <dark_aqua>" + formatTime(stats.getTotalItemsFound() > 0 ? stats.getTotalTimeSpentOnItems() / stats.getTotalItemsFound() : 0)));
        player.sendMessage(" ");

        if (!stats.getMemberStats().isEmpty()) {
            player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Contributions <dark_gray>»"));
            for (FibTeamMemberStatsDto member : stats.getMemberStats()) {
                String memberName = resolveName(member.getMemberUuid());
                player.sendMessage(mm.deserialize("    <dark_gray>» <green>" + memberName
                        + " <dark_gray>| <dark_aqua>" + member.getTotalItemsFound() + " items"
                        + " <dark_gray>| <dark_aqua>" + member.getDeaths() + " deaths"
                        + " <dark_gray>| <dark_aqua>" + member.getBlocksTravelled() + " blocks"));
            }
            player.sendMessage(" ");
        }
    }

    private void sendTopItems(Player player, List<FibItemCountDto> topItems, MiniMessage mm) {
        if (topItems == null || topItems.isEmpty()) {
            return;
        }
        for (FibItemCountDto item : topItems) {
            Material material = Material.valueOf(item.getItemName().toUpperCase());
            String unicode = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, material);
            String formattedName = this.plugin.getGamemanager().getMaterialName(material);
            player.sendMessage(mm.deserialize("    <dark_gray>» <reset>" + unicode + " <gray>" + formattedName + " <dark_gray>× <dark_aqua>" + item.getCount()));
        }
    }

    private void sendRarities(Player player, FibRaritiesDto rarities, MiniMessage mm) {
        if (rarities == null) {
            return;
        }
        long total = rarities.getRare() + rarities.getEpic() + rarities.getLegendary()
                + rarities.getRngesus() + rarities.getExtraordinary();
        if (total == 0) {
            return;
        }
        player.sendMessage(mm.deserialize("  <dark_gray>● <gray>Rarities <dark_gray>»"));
        if (rarities.getRare() > 0)
            player.sendMessage(mm.deserialize("    <dark_gray>» <blue>Rare <dark_gray>× <dark_aqua>" + rarities.getRare()));
        if (rarities.getEpic() > 0)
            player.sendMessage(mm.deserialize("    <dark_gray>» <dark_purple>Epic <dark_gray>× <dark_aqua>" + rarities.getEpic()));
        if (rarities.getLegendary() > 0)
            player.sendMessage(mm.deserialize("    <dark_gray>» <gold>Legendary <dark_gray>× <dark_aqua>" + rarities.getLegendary()));
        if (rarities.getRngesus() > 0)
            player.sendMessage(mm.deserialize("    <dark_gray>» <gradient:#E41EBC:#9A4992>RNGesus</gradient> <dark_gray>× <dark_aqua>" + rarities.getRngesus()));
        if (rarities.getExtraordinary() > 0)
            player.sendMessage(mm.deserialize("    <dark_gray>» <gradient:#73FF00:#14C8FF>Extraordinary</gradient> <dark_gray>× <dark_aqua>" + rarities.getExtraordinary()));
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

    private void sendUsage(Player player, MiniMessage mm) {
        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("<dark_gray>» <gold><b>Stats</b> <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(mm.deserialize("  <dark_gray>● <yellow>/stats solo <dark_gray>» <gray>Your solo stats"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <yellow>/stats solo <player> <dark_gray>» <gray>Solo stats of a player"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <yellow>/stats team <dark_gray>» <gray>Your overall team stats"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <yellow>/stats team <player> <dark_gray>» <gray>Overall team stats of a player"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <yellow>/stats duo <teammate> <dark_gray>» <gray>Your stats with a teammate"));
        player.sendMessage(mm.deserialize("  <dark_gray>● <yellow>/stats duo <p1> <p2> <dark_gray>» <gray>Duo stats between two players"));
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
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("duo")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }

        return completions;
    }
}
