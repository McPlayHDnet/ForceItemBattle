package forceitembattle.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.PlayerStat;
import org.apache.commons.text.WordUtils;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsManager {

    private final Gson gson;
    private final File userFile;

    private final ConcurrentSkipListMap<String, ForceItemPlayerStats> playerStats;

    public StatsManager(ForceItemBattle forceItemBattle) {
        /**
         *
         * There is a lot of improvement possible, I know.
         * I just wanted to do these stats as fast as I could. I mean it works lol
         * (hopefully)
         *
         */
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerStats = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        this.userFile = new File(forceItemBattle.getDataFolder(), "users.json");

        try {
            if (!this.userFile.exists()) this.userFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.loadStats();
    }

    public void addToStats(PlayerStat playerStat, ForceItemPlayerStats forceItemPlayerStats, int toBeAdded) {
        switch (playerStat) {
            case TOTAL_ITEMS ->
                    forceItemPlayerStats.setTotalItemsFound(forceItemPlayerStats.getTotalItemsFound() + toBeAdded);
            case TRAVELLED -> forceItemPlayerStats.setTravelled(forceItemPlayerStats.getTravelled() + toBeAdded);
            case GAMES_WON -> forceItemPlayerStats.setGamesWon(forceItemPlayerStats.getGamesWon() + toBeAdded);
            case GAMES_PLAYED -> forceItemPlayerStats.setGamesPlayed(forceItemPlayerStats.getGamesPlayed() + toBeAdded);
            case HIGHEST_SCORE -> forceItemPlayerStats.setHighestScore(toBeAdded);
        }
        this.saveStats();
    }

    public int rank(String userName) {
        int rank = -1;

        List<ForceItemPlayerStats> statsList = new ArrayList<>(List.copyOf(this.playerStats.values()));
        statsList.sort((o1, o2) -> o2.getGamesWon() <= o1.getGamesWon() ? -1 : 1);

        for (int i = 0; i < statsList.size(); i++) {
            if (statsList.get(i).getGamesWon() == this.playerStats(userName).getGamesWon()) return i + 1;
        }

        return rank;
    }

    public List<ForceItemPlayerStats> top(PlayerStat category) {
        List<ForceItemPlayerStats> statsList = new ArrayList<>(List.copyOf(this.playerStats.values()));
        statsList.sort((o1, o2) -> {
            if (category == PlayerStat.TOTAL_ITEMS) return o2.getTotalItemsFound() <= o1.getTotalItemsFound() ? -1 : 1;
            if (category == PlayerStat.GAMES_WON) return o2.getGamesWon() <= o1.getGamesWon() ? -1 : 1;
            if (category == PlayerStat.TRAVELLED) return o2.getTravelled() <= o1.getTravelled() ? -1 : 1;
            return o2.getHighestScore() <= o1.getHighestScore() ? -1 : 1;
        });

        return statsList.stream().limit(5).toList();
    }

    public boolean playerExists(String userName) {
        return this.playerStats.containsKey(userName);
    }

    public ForceItemPlayerStats playerStats(String userName) {
        return this.playerStats.get(userName);
    }

    public void resetStats(String userName) {
        ForceItemPlayerStats forceItemPlayerStats = this.playerStats.get(userName);
        forceItemPlayerStats.setTravelled(0);
        forceItemPlayerStats.setHighestScore(0);
        forceItemPlayerStats.setGamesPlayed(0);
        forceItemPlayerStats.setGamesWon(0);
        forceItemPlayerStats.setTotalItemsFound(0);

        this.playerStats.put(userName, forceItemPlayerStats);
        this.saveStats();
    }

    public void loadStats() {
        try {
            FileReader fileReader = new FileReader(this.userFile);
            Type listType = new TypeToken<ArrayList<ForceItemPlayerStats>>() {
            }.getType();
            List<ForceItemPlayerStats> statsList = this.gson.fromJson(fileReader, listType);

            if (statsList == null) return;

            if (!statsList.isEmpty()) {
                for (ForceItemPlayerStats stats : statsList) {
                    this.playerStats.put(stats.getUserName(), stats);
                }
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveStats() {
        try {
            FileWriter fileWriter = new FileWriter(this.userFile);

            List<ForceItemPlayerStats> statsList = List.copyOf(this.playerStats.values());

            this.gson.toJson(statsList, fileWriter);

            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void statsMessage(Player player, ForceItemPlayerStats forceItemPlayerStats) {
        double winPercentage = (forceItemPlayerStats.getGamesPlayed() != 0) ? ((double) forceItemPlayerStats.getGamesWon() / forceItemPlayerStats.getGamesPlayed() * 100) : 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage("§8» §6§lStats §8● §a" + forceItemPlayerStats.getUserName() + " §8«");
        player.sendMessage(" ");
        player.sendMessage("  §8● §7Rank §8» §3#" + this.rank(forceItemPlayerStats.getUserName()));
        player.sendMessage("  §8● §7Total items found §8» §3" + forceItemPlayerStats.getTotalItemsFound());
        player.sendMessage("  §8● §7Travelled §8» §3" + (int) Math.round(forceItemPlayerStats.getTravelled()) + " blocks");
        player.sendMessage("  §8● §7Highest score §8» §3" + forceItemPlayerStats.getHighestScore());
        player.sendMessage("  §8● §7Games played §8» §3" + forceItemPlayerStats.getGamesPlayed());
        player.sendMessage("  §8● §7Games won §8» §3" + forceItemPlayerStats.getGamesWon());
        player.sendMessage("  §8● §7Win percentage §8» §3" + decimalFormat.format(winPercentage) + "%");
        player.sendMessage(" ");
    }

    public void topMessage(Player player, List<ForceItemPlayerStats> topList, PlayerStat playerStat) {
        player.sendMessage(" ");
        player.sendMessage("§8» §6§lLeaderboard §8● §a" + WordUtils.capitalize(playerStat.name().toLowerCase().replace("_", " ")) + " §8«");
        player.sendMessage(" ");
        AtomicInteger atomicInteger = new AtomicInteger(1);
        topList.forEach(tops -> {
            player.sendMessage("  §8● §6" + atomicInteger.get() + ". §a" + tops.getUserName() + " §8» §3" + this.getStatByName(tops, playerStat) + (playerStat == PlayerStat.TRAVELLED ? " blocks" : ""));
            atomicInteger.getAndIncrement();
        });
        player.sendMessage(" ");
    }

    public int getStatByName(ForceItemPlayerStats forceItemPlayerStats, PlayerStat playerStat) {
        switch (playerStat) {
            case GAMES_PLAYED -> {
                return forceItemPlayerStats.getGamesPlayed();
            }
            case GAMES_WON -> {
                return forceItemPlayerStats.getGamesWon();
            }
            case HIGHEST_SCORE -> {
                return forceItemPlayerStats.getHighestScore();
            }
            case TOTAL_ITEMS -> {
                return forceItemPlayerStats.getTotalItemsFound();
            }
            case TRAVELLED -> {
                return (int) Math.round(forceItemPlayerStats.getTravelled());
            }
        }
        return -1;
    }

    public int calculateDistance(Player player) {
        int distance = 0;

        for (Statistic statistics : Statistic.values()) {
            //check and get every statistic that has CM (distance based)
            if (statistics.name().contains("CM")) {
                distance += player.getStatistic(statistics);
            }
        }

        return (int) Math.round((double) distance / 100);
    }

    public void createPlayerStats(ForceItemPlayer forceItemPlayer) {
        if (this.playerExists(forceItemPlayer.getPlayer().getName())) return;

        ForceItemPlayerStats forceItemPlayerStats = new ForceItemPlayerStats(forceItemPlayer.getPlayer().getName(), 0, 0.0, 0, 0, 0);

        this.playerStats.put(forceItemPlayer.getPlayer().getName(), forceItemPlayerStats);
        this.saveStats();
    }

}
