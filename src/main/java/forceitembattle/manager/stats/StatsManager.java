package forceitembattle.manager.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.PlayerStat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.Player;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StatsManager {

    private static final String STATS_DIR = ForceItemBattle.getInstance().getDataFolder() + "/stats/";

    public static final String CURRENT_SEASON = "1";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, ForceItemPlayerStats> playerStats = new HashMap<>();

    public StatsManager() {
        this.ensureDirectories();
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(Paths.get(STATS_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean statsExist(String playerName) {
        String filePath = STATS_DIR + playerName + ".json";
        return Files.exists(Paths.get(filePath));
    }

    public boolean hasSeason(String playerName, String season) {
        ForceItemPlayerStats stats = this.loadPlayerStats(playerName);
        if (stats != null) {
            return stats.hasSeason(season);
        }
        return false;
    }

    private void createStats(ForceItemPlayerStats stats, String season) {
        SeasonalStats currentSeasonStats = new SeasonalStats();
        stats.setSeasonStats(season, currentSeasonStats);
    }

    public void resetStats(ForceItemPlayerStats stats, String season) {
        this.createStats(stats, season);
    }

    public ForceItemPlayerStats loadPlayerStats(String playerName) {
        String filePath = STATS_DIR + playerName + ".json";
        ForceItemPlayerStats stats;

        try {
            if (Files.exists(Paths.get(filePath))) {
                FileReader reader = new FileReader(filePath);
                stats = gson.fromJson(reader, ForceItemPlayerStats.class);
                this.playerStats.put(playerName, stats);

                if (!stats.hasSeason(CURRENT_SEASON)) {
                    createStats(stats, CURRENT_SEASON);
                    savePlayerStats(playerName, stats);
                }
            } else {
                stats = new ForceItemPlayerStats(playerName);
                createStats(stats, CURRENT_SEASON);
                savePlayerStats(playerName, stats);
            }

            if (stats.getSeasonStats(CURRENT_SEASON) == null) {
                createStats(stats, CURRENT_SEASON);
                savePlayerStats(playerName, stats);
            }

            return stats;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ForceItemPlayerStats> loadAllPlayerStats() {
        List<ForceItemPlayerStats> statsList = new ArrayList<>();

        try {
            Files.walk(Paths.get(STATS_DIR)).filter(Files::isRegularFile).forEach(path -> {
                try {
                    FileReader reader = new FileReader(path.toFile());
                    ForceItemPlayerStats stats = gson.fromJson(reader, ForceItemPlayerStats.class);
                    statsList.add(stats);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return statsList;
    }

    public void savePlayerStats(String playerName, ForceItemPlayerStats stats) {
        String filePath = STATS_DIR + playerName + ".json";
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(stats, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateSoloStats(String playerName, PlayerStat stat, int value) {
        ForceItemPlayerStats playerStats = this.loadPlayerStats(playerName);
        this.updateSoloStat(playerStats.getSeasonStats(CURRENT_SEASON), stat, value);
        this.savePlayerStats(playerName, playerStats);
    }

    public void updateTeamStats(String player1Name, String player2Name, int value, PlayerStat statType) {
        ForceItemPlayerStats player1Stats = this.loadPlayerStats(player1Name);
        ForceItemPlayerStats player2Stats = this.loadPlayerStats(player2Name);

        player1Stats.getSeasonStats(CURRENT_SEASON).updateTeamStats(player2Name, value, statType);
        player2Stats.getSeasonStats(CURRENT_SEASON).updateTeamStats(player1Name, value, statType);

        savePlayerStats(player1Name, player1Stats);
        savePlayerStats(player2Name, player2Stats);
    }

    private void updateSoloStat(SeasonalStats seasonStats, PlayerStat stat, int value) {
        switch (stat) {
            case GAMES_PLAYED:
                seasonStats.getGamesPlayed().setSolo(seasonStats.getGamesPlayed().getSolo() + value);
                break;
            case GAMES_WON:
                seasonStats.getGamesWon().setSolo(seasonStats.getGamesWon().getSolo() + value);
                break;
            case HIGHEST_SCORE:
                seasonStats.getHighestScore().setSolo(value);
                break;
            case BACK_TO_BACK_STREAK:
                seasonStats.getBack2backStreak().setSolo(value);
                break;
            case TOTAL_ITEMS:
                seasonStats.getTotalItemsFound().setSolo(seasonStats.getTotalItemsFound().getSolo() + value);
                break;
            case WIN_STREAK:
                seasonStats.getWinStreak().setSolo(seasonStats.getWinStreak().getSolo() + value);
                break;
            case TRAVELLED:
                seasonStats.setTravelled(seasonStats.getTravelled() + value);
                break;
        }
    }

    public void statsMessage(Player player, ForceItemPlayerStats forceItemPlayerStats, String season) {
        SeasonalStats seasonalStats = forceItemPlayerStats.getSeasonStats(season);
        double winPercentage = (seasonalStats.getGamesPlayed().getSolo() != 0) ? ((double) seasonalStats.getGamesWon().getSolo() / seasonalStats.getGamesPlayed().getSolo() * 100) : 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.#");

        MiniMessage miniMessage = ForceItemBattle.getInstance().getGamemanager().getMiniMessage();

        int rank = this.rank(forceItemPlayerStats.getUserName(), season);

        player.sendMessage(" ");
        player.sendMessage(miniMessage.deserialize("<dark_gray>» <gold><b>Stats</b> <dark_gray>● <green>" + forceItemPlayerStats.getUserName() + " <dark_gray>«"));
        player.sendMessage(miniMessage.deserialize("        <gray>« <dark_aqua>Season <gold>" + season + " <gray>»"));
        player.sendMessage(" ");
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Rank <dark_gray>» <dark_aqua>" + rank));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + seasonalStats.getTotalItemsFound().getSolo()));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + (int) Math.round(seasonalStats.getTravelled()) + " blocks"));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + seasonalStats.getHighestScore().getSolo()));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + seasonalStats.getBack2backStreak().getSolo()));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + seasonalStats.getGamesPlayed().getSolo()));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + seasonalStats.getGamesWon().getSolo()));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Win streak <dark_gray>» <dark_aqua>" + seasonalStats.getWinStreak().getSolo()));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + decimalFormat.format(winPercentage) + "%"));
        player.sendMessage(" ");
    }

    public void teamStatsMessage(Player player, ForceItemPlayerStats forceItemPlayerStats, String season) {
        SeasonalStats seasonalStats = forceItemPlayerStats.getSeasonStats(season);
        double winPercentage = (seasonalStats.getGamesPlayed().getSolo() != 0) ? ((double) seasonalStats.getGamesWon().getSolo() / seasonalStats.getGamesPlayed().getSolo() * 100) : 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.#");

        MiniMessage miniMessage = ForceItemBattle.getInstance().getGamemanager().getMiniMessage();

        player.sendMessage(" ");
        player.sendMessage(miniMessage.deserialize("<dark_gray>» <gold><b>Team-Stats</b> <dark_gray>● <green>" + forceItemPlayerStats.getUserName() + " <dark_gray>«"));
        player.sendMessage(miniMessage.deserialize("        <gray>« <dark_aqua>Season <gold>" + season + " <gray>»"));
        player.sendMessage(" ");
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Rank <dark_gray>» <dark_aqua>N/A"));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + this.displayTeamStat(seasonalStats.getTotalItemsFound())));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + (int) Math.round(seasonalStats.getTravelled()) + " blocks"));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + this.displayTeamStat(seasonalStats.getHighestScore())));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + this.displayTeamStat(seasonalStats.getBack2backStreak())));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + this.displayTeamStat(seasonalStats.getGamesPlayed())));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + this.displayTeamStat(seasonalStats.getGamesWon())));
        player.sendMessage(miniMessage.deserialize("  <dark_gray>● <gray>Win streak <dark_gray>» <dark_aqua>" + this.displayTeamStat(seasonalStats.getWinStreak())));
        player.sendMessage(" ");
    }

    public int rank(String playerName, String season) {
        int rank = -1;

        List<ForceItemPlayerStats> statsList = loadAllPlayerStats();
        statsList.sort((o1, o2) -> Integer.compare(
                o2.getSeasonStats(season).getGamesWon().getSolo(),
                o1.getSeasonStats(season).getGamesWon().getSolo()
        ));

        int currentRank = 0;
        int previousGamesWon = -1;

        for (int i = 0; i < statsList.size(); i++) {
            ForceItemPlayerStats stats = statsList.get(i);
            int gamesWon = stats.getSeasonStats(season).getGamesWon().getSolo();

            if (gamesWon != previousGamesWon) {
                currentRank = i + 1;
                previousGamesWon = gamesWon;
            }

            if (stats.getUserName().equals(playerName)) {
                rank = currentRank;
                break;
            }
        }

        return rank;
    }

    public void topMessage(Player player, List<ForceItemPlayerStats> topList, PlayerStat playerStat) {
        player.sendMessage(" ");
        player.sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold><b>Leaderboard</b> <dark_gray>● <green>" + WordUtils.capitalize(playerStat.name().toLowerCase().replace("_", " ")) + " <dark_gray>«"));
        player.sendMessage(" ");
        AtomicInteger atomicInteger = new AtomicInteger(1);
        topList.forEach(tops -> {
            player.sendMessage(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize("  <dark_gray>● " + this.placeColor(atomicInteger.get()) + atomicInteger.get() + "<white>. <green>" + tops.getUserName() + " <dark_gray>» <dark_aqua>" + this.getStatByName(tops, playerStat) + (playerStat == PlayerStat.TRAVELLED ? " blocks" : "")));
            atomicInteger.getAndIncrement();
        });
        player.sendMessage(" ");
    }

    private String placeColor(int place) {
        return switch (place) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<dark_gray>";
            default -> "<white>";
        };
    }

    private int getStatByName(ForceItemPlayerStats stats, PlayerStat playerStat) {
        SeasonalStats seasonalStats = stats.getSeasonStats(CURRENT_SEASON);
        return switch (playerStat) {
            case GAMES_PLAYED -> seasonalStats.getGamesPlayed().getSolo();
            case GAMES_WON -> seasonalStats.getGamesWon().getSolo();
            case HIGHEST_SCORE -> seasonalStats.getHighestScore().getSolo();
            case BACK_TO_BACK_STREAK -> seasonalStats.getBack2backStreak().getSolo();
            case TOTAL_ITEMS -> seasonalStats.getTotalItemsFound().getSolo();
            case WIN_STREAK -> seasonalStats.getWinStreak().getSolo();
            case TRAVELLED -> (int) Math.round(seasonalStats.getTravelled());
        };
    }

    public List<ForceItemPlayerStats> top(PlayerStat category) {
        List<ForceItemPlayerStats> statsList = new ArrayList<>(List.copyOf(this.playerStats.values()));
        statsList.sort((o1, o2) -> {
            SeasonalStats oSS1 = o1.getSeasonStats(CURRENT_SEASON);
            SeasonalStats oSS2 = o2.getSeasonStats(CURRENT_SEASON);
            if (category == PlayerStat.TOTAL_ITEMS) return oSS2.getTotalItemsFound().getSolo() <= oSS1.getTotalItemsFound().getSolo() ? -1 : 1;
            if (category == PlayerStat.GAMES_WON) return oSS2.getGamesWon().getSolo() <= oSS1.getGamesWon().getSolo() ? -1 : 1;
            if (category == PlayerStat.TRAVELLED) return oSS2.getTravelled() <= oSS1.getTravelled() ? -1 : 1;
            if (category == PlayerStat.BACK_TO_BACK_STREAK) return oSS2.getBack2backStreak().getSolo() <= oSS1.getBack2backStreak().getSolo() ? -1 : 1;
            if (category == PlayerStat.WIN_STREAK) return oSS2.getWinStreak().getSolo() <= oSS1.getWinStreak().getSolo() ? -1 : 1;
            return oSS2.getHighestScore().getSolo() <= oSS1.getHighestScore().getSolo() ? -1 : 1;
        });
        return statsList.stream().limit(10).collect(Collectors.toList());
    }

    private String displayTeamStat(SeasonalStats.GameStats gameStats) {
        if (gameStats.getTeam().isEmpty()) {
            return "N/A";
        }

        Map.Entry<String, SeasonalStats.TeamStat> highestTeamStat = gameStats.getTeam().entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingInt(SeasonalStats.TeamStat::getValue)))
                .orElse(null);

        if (highestTeamStat != null) {
            String teamName = highestTeamStat.getKey();
            SeasonalStats.TeamStat teamStat = highestTeamStat.getValue();

            return teamName + " <dark_gray>| <gold>" + teamStat.getValue() + " <dark_gray><i>(" + teamStat.getLastUpdated() + ")";
        }
        return null;
    }
}
