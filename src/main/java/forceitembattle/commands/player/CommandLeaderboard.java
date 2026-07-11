package forceitembattle.commands.player;

import de.threeseconds.openapi.fibservice.client.model.FibAchievementLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibTeamLeaderboardEntryDto;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CommandLeaderboard extends CustomCommand implements CustomTabCompleter {

    private static final int TOP_LIMIT = 10;

    private static final String DEFAULT_CATEGORY = "highest_score";

    // Scope selectors that mirror the /stats views: solo, duo (a team pair), teams
    // (a player's combined across-all-teams stats).
    private static final List<String> SCOPES = List.of("solo", "duo", "teams", "achievements");

    // The achievement board ranks players by unlocked-achievement count, so it takes no
    // category and shows a short podium rather than the usual ten rows.
    private static final int ACHIEVEMENT_TOP_LIMIT = 3;

    private static final List<String> CATEGORIES = List.of(
            "highest_score", "total_items", "games_won", "back_to_back_streak", "blocks_travelled"
    );

    public CommandLeaderboard(ForceItemBattle plugin) {
        super(plugin, "top");
        setUsage("[solo|duo|teams|achievements] [stat]");
        setDescription("Show the stat leaderboards");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        FibStatisticsClient helper = this.plugin.getFibService().statistics();

        // The first argument is always the scope: solo, duo or teams. Omitting it
        // falls back to the solo leaderboard, so a bare "/top" still works.
        String scope = args.length >= 1 ? args[0].toLowerCase() : "solo";
        if (!SCOPES.contains(scope)) {
            player.sendMessage(Text.of("<yellow>" + args[0] + " <red>is not a valid scope. Use <white>solo<red>, <white>duo<red>, <white>teams <red>or <white>achievements<red>."));
            return;
        }

        // "achievements" ranks by achievement count and takes no category argument.
        if (scope.equals("achievements")) {
            showAchievementLeaderboard(player);
            return;
        }

        String category = args.length >= 2 ? args[1].toLowerCase() : DEFAULT_CATEGORY;
        if (!CATEGORIES.contains(category)) {
            player.sendMessage(Text.of("<yellow>" + category + " <red>does not exist in leaderboard"));
            return;
        }

        switch (scope) {
            case "duo" -> showDuoLeaderboard(player, helper, category);
            case "teams" -> showTeamsLeaderboard(player, helper, category);
            default -> showSoloLeaderboard(player, helper, category);
        }
    }

    private void showSoloLeaderboard(Player player, FibStatisticsClient helper, String category) {
        helper.getSoloLeaderboardAsync(category, TOP_LIMIT, entries -> {
            sendHeader(player, "Leaderboard", category);
            if (entries.isEmpty()) {
                sendEmpty(player);
            } else {
                for (FibLeaderboardEntryDto entry : entries) {
                    sendRow(player, entry.getRank(), resolvePlayerName(entry.getPlayerUuid()), entry.getValue(), suffixFor(category));
                }
            }
            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void showTeamsLeaderboard(Player player, FibStatisticsClient helper, String category) {
        helper.getCombinedTeamLeaderboardAsync(category, TOP_LIMIT, entries -> {
            sendHeader(player, "Team Leaderboard", category);
            if (entries.isEmpty()) {
                sendEmpty(player);
            } else {
                for (FibLeaderboardEntryDto entry : entries) {
                    sendRow(player, entry.getRank(), resolvePlayerName(entry.getPlayerUuid()), entry.getValue(), suffixFor(category));
                }
            }
            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void showDuoLeaderboard(Player player, FibStatisticsClient helper, String category) {
        helper.getTeamLeaderboardAsync(category, TOP_LIMIT, entries -> {
            sendHeader(player, "Duo Leaderboard", category);
            if (entries.isEmpty()) {
                sendEmpty(player);
            } else {
                for (FibTeamLeaderboardEntryDto entry : entries) {
                    String names = resolvePlayerName(entry.getPlayer1Uuid())
                            + " <dark_gray>& <green>" + resolvePlayerName(entry.getPlayer2Uuid());
                    sendRow(player, entry.getRank(), names, entry.getValue(), suffixFor(category));
                }
            }
            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void showAchievementLeaderboard(Player player) {
        this.plugin.getFibService().achievements().getAchievementLeaderboardAsync(ACHIEVEMENT_TOP_LIMIT, entries -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<dark_gray>» <gold><b>Leaderboard</b> <dark_gray>● <green>Achievements <dark_gray>«"));
            player.sendMessage(" ");

            if (entries.isEmpty()) {
                sendEmpty(player);
            } else {
                for (FibAchievementLeaderboardEntryDto entry : entries) {
                    int count = entry.getCount();
                    sendRow(player, entry.getRank(), resolvePlayerName(entry.getPlayerUuid()), count,
                            count == 1 ? " achievement" : " achievements");
                }
            }

            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void sendHeader(Player player, String title, String category) {
        player.sendMessage(" ");
        player.sendMessage(Text.of("<dark_gray>» <gold><b>" + title + "</b> <dark_gray>● <green>"
                + formatCategoryName(category) + " <dark_gray>«"));
        player.sendMessage(" ");
    }

    private void sendRow(Player player, int rank, String name, long value, String suffix) {
        String color = switch (rank) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<dark_gray>";
            default -> "<white>";
        };
        player.sendMessage(Text.of("  <dark_gray>● " + color + rank + "<white>. <green>"
                + name + " <dark_gray>» <dark_aqua>" + value + suffix));
    }

    private String suffixFor(String category) {
        return category.equals("blocks_travelled") ? " blocks" : "";
    }

    private void sendEmpty(Player player) {
        player.sendMessage(Text.of("  <gray>No entries yet."));
    }

    private void sendError(Player player) {
        player.sendMessage(Text.of("<red>Could not load leaderboard."));
    }

    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private String formatCategoryName(String category) {
        String[] words = category.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(SCOPES);
        }
        // "achievements" takes no category, so offer nothing for its second argument.
        if (args.length == 2 && SCOPES.contains(args[0].toLowerCase())
                && !args[0].equalsIgnoreCase("achievements")) {
            return new ArrayList<>(CATEGORIES);
        }
        return new ArrayList<>();
    }
}
