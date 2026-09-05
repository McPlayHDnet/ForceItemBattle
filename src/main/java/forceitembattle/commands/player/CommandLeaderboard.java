package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.model.stats.DuoLeaderboardEntry;
import forceitembattle.model.stats.LeaderboardEntry;
import forceitembattle.model.stats.PlayerIdentity;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandLeaderboard extends CustomCommand implements CustomTabCompleter {

    private static final int TOP_LIMIT = 10;

    private static final String DEFAULT_CATEGORY = "highest_score";

    // Scope selectors that mirror the /stats views: solo, duo (a team pair), teams
    // (a player's combined across-all-teams stats).
    private static final List<String> SCOPES = List.of("solo", "duo", "teams", "achievements");

    private static final List<String> CATEGORIES = List.of(
            "highest_score", "total_items", "games_won", "back_to_back_streak", "blocks_travelled"
    );

    private final FIBServiceClient fibService;

    public CommandLeaderboard(FIBServiceClient fibService) {
        super("top");
        this.fibService = fibService;
        setUsage("[solo|duo|teams|achievements] [stat]");
        setDescription("Show the stat leaderboards");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        FibStatisticsClient helper = this.fibService.statistics();

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
        helper.soloLeaderboard(category, TOP_LIMIT, entries -> {
            sendHeader(player, "Leaderboard", category);
            if (entries.isEmpty()) {
                sendEmpty(player);
                player.sendMessage(" ");
                return;
            }

            // Names arrive inside each entry now, so there is no lookup to do -- the row renders
            // straight from the payload.
            for (LeaderboardEntry entry : entries) {
                sendRow(player, entry.rank(), PlayerIdentity.displayName(entry.player(), "?"),
                        entry.value(), suffixFor(category));
            }
            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void showTeamsLeaderboard(Player player, FibStatisticsClient helper, String category) {
        helper.combinedTeamLeaderboard(category, TOP_LIMIT, entries -> {
            sendHeader(player, "Teams Leaderboard", category);
            if (entries.isEmpty()) {
                sendEmpty(player);
                player.sendMessage(" ");
                return;
            }

            for (LeaderboardEntry entry : entries) {
                sendRow(player, entry.rank(), PlayerIdentity.displayName(entry.player(), "?"),
                        entry.value(), suffixFor(category));
            }
            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void showDuoLeaderboard(Player player, FibStatisticsClient helper, String category) {
        helper.duoLeaderboard(category, TOP_LIMIT, entries -> {
            sendHeader(player, "Duo Leaderboard", category);
            if (entries.isEmpty()) {
                sendEmpty(player);
                player.sendMessage(" ");
                return;
            }

            for (DuoLeaderboardEntry entry : entries) {
                String pair = PlayerIdentity.displayName(entry.player1(), "?")
                        + " <dark_gray>& <green>" + PlayerIdentity.displayName(entry.player2(), "?");
                sendRow(player, entry.rank(), pair, entry.value(), suffixFor(category));
            }
            player.sendMessage(" ");
        }, error -> sendError(player));
    }

    private void showAchievementLeaderboard(Player player) {
        this.fibService.achievements().achievementLeaderboard(TOP_LIMIT, entries -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<dark_gray>» <gold><b>Leaderboard</b> <dark_gray>● <green>Achievements <dark_gray>«"));
            player.sendMessage(" ");

            if (entries.isEmpty()) {
                sendEmpty(player);
                player.sendMessage(" ");
                return;
            }

            for (LeaderboardEntry entry : entries) {
                sendRow(player, entry.rank(), PlayerIdentity.displayName(entry.player(), "?"),
                        entry.value(), "");
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

    /** Turns a snake_case category key into a spaced, title-cased header label. */
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
            return SCOPES;
        }
        // "achievements" takes no category, so offer nothing for its second argument.
        if (args.length == 2 && SCOPES.contains(args[0].toLowerCase())
                && !args[0].equalsIgnoreCase("achievements")) {
            return CATEGORIES;
        }
        return List.of();
    }
}
