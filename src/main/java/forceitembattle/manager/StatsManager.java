package forceitembattle.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.PlayerStat;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class StatsManager {

    /**
     *
     * There is a lot of improvement possible, I know.
     * I just wanted to do these stats as fast as I could. I mean it works lol
     * (hopefully)
     *
     */

    private final ForceItemBattle forceItemBattle;
    private final Gson gson;
    private File userFile;

    private ConcurrentSkipListMap<String, ForceItemPlayerStats> playerStats;

    public StatsManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerStats = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        this.userFile = new File(this.forceItemBattle.getDataFolder(), "users.json");

        try {
            if(!this.userFile.exists()) this.userFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.loadStats();
    }

    public void addToStats(PlayerStat playerStat, ForceItemPlayerStats forceItemPlayerStats, int toBeAdded) {
        switch(playerStat) {
            case TOTAL_ITEMS -> forceItemPlayerStats.setTotalItemsFound(forceItemPlayerStats.totalItemsFound() + toBeAdded);
            case GAMES_WON -> forceItemPlayerStats.setGamesWon(forceItemPlayerStats.gamesWon() + toBeAdded);
            case GAMES_PLAYED -> forceItemPlayerStats.setGamesPlayed(forceItemPlayerStats.gamesPlayed() + toBeAdded);
            case HIGHEST_SCORE -> forceItemPlayerStats.setHighestScore(forceItemPlayerStats.highestScore() + toBeAdded);
        }
        this.saveStats();
    }

    public int rank(String userName) {
        int rank = -1;

        List<ForceItemPlayerStats> statsList = new ArrayList<>(List.copyOf(this.playerStats.values()));
        statsList.sort((o1, o2) -> o2.gamesWon() <= o1.gamesWon() ? -1 : 1);

        for(int i = 0; i < statsList.size(); i++) {
            if(statsList.get(i).gamesWon() == this.playerStats(userName).gamesWon()) return i + 1;
        }

        return rank;
    }

    public boolean playerExists(String userName) {
        return this.playerStats.containsKey(userName);
    }

    public ForceItemPlayerStats playerStats(String userName) {
        return this.playerStats.get(userName);
    }

    public void loadStats() {
        try {
            FileReader fileReader = new FileReader(this.userFile);
            Type listType = new TypeToken<ArrayList<ForceItemPlayerStats>>(){}.getType();
            List<ForceItemPlayerStats> statsList = this.gson.fromJson(fileReader, listType);

            if(statsList == null) return;

            if(!statsList.isEmpty()) {
                for(ForceItemPlayerStats stats : statsList) {
                    this.playerStats.put(stats.userName(), stats);
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
        player.sendMessage(" ");
        player.sendMessage("§8» §6§lStats §8● §a" + forceItemPlayerStats.userName() + " §8«");
        player.sendMessage(" ");
        player.sendMessage("  §8● §7Rank §8» §3#" + this.rank(forceItemPlayerStats.userName()));
        player.sendMessage("  §8● §7Total items found §8» §3" + forceItemPlayerStats.totalItemsFound());
        player.sendMessage("  §8● §7Highest score §8» §3" + forceItemPlayerStats.highestScore());
        player.sendMessage("  §8● §7Games played §8» §3" + forceItemPlayerStats.gamesPlayed());
        player.sendMessage("  §8● §7Games won §8» §3" + forceItemPlayerStats.gamesWon());
        player.sendMessage("  §8● §7Win percentage §8» §3" + (forceItemPlayerStats.gamesPlayed() != 0 ? ((forceItemPlayerStats.gamesWon() / forceItemPlayerStats.gamesPlayed()) * 100) : 0) + "%");
        player.sendMessage(" ");
    }

    public void createPlayerStats(ForceItemPlayer forceItemPlayer) {
        if(this.playerExists(forceItemPlayer.player().getName())) return;

        ForceItemPlayerStats forceItemPlayerStats = new ForceItemPlayerStats(forceItemPlayer.player().getName(), 0, 0, 0, 0);

        this.playerStats.put(forceItemPlayer.player().getName(), forceItemPlayerStats);
        this.saveStats();
    }

}
