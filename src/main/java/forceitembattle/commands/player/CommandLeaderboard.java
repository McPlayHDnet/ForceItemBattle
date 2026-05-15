package forceitembattle.commands.player;

import de.threeseconds.openapi.fibservice.client.model.FibLeaderboardEntryDto;
import de.threeseconds.openapi.fibservice.client.model.FibSoloStatisticsDto;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.stats.FIBServiceHelper;
import forceitembattle.util.PlayerStat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandLeaderboard extends CustomCommand implements CustomTabCompleter {

    private static final int TOP_LIMIT = 10;

    private static final List<String> CATEGORIES = List.of(
            "highest_score", "total_items", "games_won", "back_to_back_streak", "blocks_travelled"
    );

    public CommandLeaderboard() {
        super("top");
        setUsage("[stat]");
        setDescription("Show the stat leaderboards");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        MiniMessage mm = this.plugin.getGamemanager().getMiniMessage();
        FIBServiceHelper helper = this.plugin.getFibServiceHelper();

        String category = args.length >= 1 ? args[0].toLowerCase() : "highest_score";

        if (!CATEGORIES.contains(category)) {
            player.sendMessage(mm.deserialize("<yellow>" + args[0] + " <red>does not exist in leaderboard"));
            return;
        }

        helper.getSoloLeaderboardAsync(category, TOP_LIMIT, entries -> {
            String displayName = formatCategoryName(category);

            player.sendMessage(" ");
            player.sendMessage(mm.deserialize("<dark_gray>» <gold><b>Leaderboard</b> <dark_gray>● <green>" + displayName + " <dark_gray>«"));
            player.sendMessage(" ");

            if (entries.isEmpty()) {
                player.sendMessage(mm.deserialize("  <gray>No entries yet."));
            } else {
                for (FibLeaderboardEntryDto entry : entries) {
                    int rank = entry.getRank();
                    String color = switch (rank) {
                        case 1 -> "<gold>";
                        case 2 -> "<gray>";
                        case 3 -> "<dark_gray>";
                        default -> "<white>";
                    };
                    String name = resolvePlayerName(entry.getPlayerUuid());
                    String suffix = category.equals("blocks_travelled") ? " blocks" : "";
                    player.sendMessage(mm.deserialize("  <dark_gray>● " + color + rank + "<white>. <green>"
                            + name + " <dark_gray>» <dark_aqua>" + entry.getValue() + suffix));
                }
            }

            player.sendMessage(" ");
        }, error -> {
            player.sendMessage(mm.deserialize("<red>Could not load leaderboard."));
        });
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
        return new ArrayList<>(CATEGORIES);
    }
}
