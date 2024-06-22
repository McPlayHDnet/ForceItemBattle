package forceitembattle.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.ForceItemPlayerStats;
import forceitembattle.util.PlayerStat;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
            case TOTAL_ITEMS -> {
                forceItemPlayerStats.setTotalItemsFound(forceItemPlayerStats.totalItemsFound() + toBeAdded);
            }
            case TRAVELLED -> forceItemPlayerStats.setTravelled(forceItemPlayerStats.travelled() + toBeAdded);
            case GAMES_WON -> forceItemPlayerStats.setGamesWon(forceItemPlayerStats.gamesWon() + toBeAdded);
            case GAMES_PLAYED -> forceItemPlayerStats.setGamesPlayed(forceItemPlayerStats.gamesPlayed() + toBeAdded);
            case HIGHEST_SCORE -> forceItemPlayerStats.setHighestScore(toBeAdded);
            case WIN_STREAK -> forceItemPlayerStats.setWinStreak(toBeAdded);
            case BACK_TO_BACK_STREAK -> forceItemPlayerStats.setBack2backStreak(toBeAdded);
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

    public List<ForceItemPlayerStats> top(PlayerStat category) {
        List<ForceItemPlayerStats> statsList = new ArrayList<>(List.copyOf(this.playerStats.values()));
        statsList.sort((o1, o2) -> {
            if(category == PlayerStat.TOTAL_ITEMS) return o2.totalItemsFound() <= o1.totalItemsFound() ? -1 : 1;
            if(category == PlayerStat.GAMES_WON) return o2.gamesWon() <= o1.gamesWon() ? -1 : 1;
            if(category == PlayerStat.TRAVELLED) return o2.travelled() <= o1.travelled() ? -1 : 1;
            if(category == PlayerStat.BACK_TO_BACK_STREAK) return o2.back2backStreak() <= o1.back2backStreak() ? -1 : 1;
            if(category == PlayerStat.WIN_STREAK) return o2.winStreak() <= o1.winStreak() ? -1 : 1;
            return o2.highestScore() <= o1.highestScore() ? -1 : 1;
        });
        return statsList.stream().limit(10).collect(Collectors.toList());
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
        forceItemPlayerStats.setBack2backStreak(0);
        forceItemPlayerStats.setWinStreak(0);
        forceItemPlayerStats.setAchievementsDone(new ArrayList<>());
        forceItemPlayerStats.setMostFoundItems(new TreeMap<>(Comparator.reverseOrder()));

        this.playerStats.put(userName, forceItemPlayerStats);
        this.saveStats();
    }

    public void resetAchievements(String userName) {
        ForceItemPlayerStats forceItemPlayerStats = this.playerStats.get(userName);
        forceItemPlayerStats.setAchievementsDone(new ArrayList<>());

        this.playerStats.put(userName, forceItemPlayerStats);
        this.saveStats();
    }

    public void loadStats() {
        try {
            FileReader fileReader = new FileReader(this.userFile);
            Type listType = new TypeToken<ArrayList<ForceItemPlayerStats>>(){}.getType();
            List<ForceItemPlayerStats> statsList = this.gson.fromJson(fileReader, listType);

            if(statsList == null) return;

            if(!statsList.isEmpty()) {
                for(ForceItemPlayerStats stats : statsList) {
                    this.ensureAllKeysPresent(stats);
                    this.playerStats.put(stats.userName(), stats);
                }
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureAllKeysPresent(ForceItemPlayerStats playerStats) {
        try {
            for (Field field : ForceItemPlayerStats.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(playerStats) == null) {
                    if (field.getType().equals(int.class)) {
                        field.set(playerStats, 0);
                    } else if (field.getType().equals(double.class)) {
                        field.set(playerStats, 0.0);
                    } else if (field.getType().equals(String.class)) {
                        field.set(playerStats, "");
                    } else if (field.getType().equals(List.class)) {
                        field.set(playerStats, new ArrayList<>());
                    } else if (field.getType().equals(SortedMap.class)) {
                        field.set(playerStats, new TreeMap<>(Comparator.reverseOrder()));
                    }
                }
            }
        } catch (IllegalAccessException e) {
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
        double winPercentage = (forceItemPlayerStats.gamesPlayed() != 0) ? ((double) forceItemPlayerStats.gamesWon() / forceItemPlayerStats.gamesPlayed() * 100) : 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.#");

        player.sendMessage(" ");
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold><b>Stats</b> <dark_gray>● <green>" + forceItemPlayerStats.userName() + " <dark_gray>«"));
        player.sendMessage(" ");
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Rank <dark_gray>» <dark_aqua>#" + this.rank(forceItemPlayerStats.userName())));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Total items found <dark_gray>» <dark_aqua>" + forceItemPlayerStats.totalItemsFound()));
        this.getTopFoundItems(forceItemPlayerStats.userName()).forEach((material, count) -> {
            player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("     <dark_gray>➥ <reset>" + this.forceItemBattle.getItemDifficultiesManager().getUnicodeFromMaterial(true, material) + "   <gray>" + this.forceItemBattle.getGamemanager().getMaterialName(material) + " <dark_gray>» <gold>x" + count));
        });
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Travelled <dark_gray>» <dark_aqua>" + (int)Math.round(forceItemPlayerStats.travelled()) + " blocks"));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Highest score <dark_gray>» <dark_aqua>" + forceItemPlayerStats.highestScore()));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Back-to-Back streak <dark_gray>» <dark_aqua>" + forceItemPlayerStats.back2backStreak()));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Games played <dark_gray>» <dark_aqua>" + forceItemPlayerStats.gamesPlayed()));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Games won <dark_gray>» <dark_aqua>" + forceItemPlayerStats.gamesWon()));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Win streak <dark_gray>» <dark_aqua>" + forceItemPlayerStats.winStreak()));
        player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● <gray>Win percentage <dark_gray>» <dark_aqua>" + decimalFormat.format(winPercentage) + "%"));
        player.sendMessage(" ");
    }

    public void topMessage(Player player, List<ForceItemPlayerStats> topList, PlayerStat playerStat) {
        player.sendMessage(" ");
        player.sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize("<dark_gray>» <gold><b>Leaderboard</b> <dark_gray>● <green>" + WordUtils.capitalize(playerStat.name().toLowerCase().replace("_", " ")) + " <dark_gray>«"));
        player.sendMessage(" ");
        AtomicInteger atomicInteger = new AtomicInteger(1);
        topList.forEach(tops -> {
            player.sendMessage(forceItemBattle.getGamemanager().getMiniMessage().deserialize("  <dark_gray>● " + this.placeColor(atomicInteger.get()) + atomicInteger.get() + "<white>. <green>" + tops.userName() + " <dark_gray>» <dark_aqua>" + this.getStatByName(tops, playerStat) + (playerStat == PlayerStat.TRAVELLED ? " blocks" : "")));
            atomicInteger.getAndIncrement();
        });
        player.sendMessage(" ");
    }

    public Map<Material, Integer> getTopFoundItems(String userName) {
        ForceItemPlayerStats playerStats = this.playerStats(userName);
        if (playerStats == null) {
            return new TreeMap<>();
        }

        Map<Material, Integer> mostFoundItems = playerStats.mostFoundItems();

        return mostFoundItems.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public String placeColor(int place) {
        String placeColor;
        switch(place) {
            case 3 -> placeColor = "<red>";
            case 2 -> placeColor = "<gray>";
            case 1 -> placeColor = "<gold>";
            default -> placeColor = "<white>";
        }
        return placeColor;
    }

    public int getStatByName(ForceItemPlayerStats forceItemPlayerStats, PlayerStat playerStat) {
        switch(playerStat) {
            case GAMES_WON -> {
                return forceItemPlayerStats.gamesWon();
            }
            case HIGHEST_SCORE -> {
                return forceItemPlayerStats.highestScore();
            }
            case TOTAL_ITEMS -> {
                return forceItemPlayerStats.totalItemsFound();
            }
            case WIN_STREAK -> {
                return forceItemPlayerStats.winStreak();
            }
            case BACK_TO_BACK_STREAK -> {
                return forceItemPlayerStats.back2backStreak();
            }
            case TRAVELLED -> {
                return (int)Math.round(forceItemPlayerStats.travelled());
            }
        }
        return -1;
    }

    public int calculateDistance(Player player) {
        int distance = 0;

        for(Statistic statistics : Statistic.values()) {
            //check and get every statistic that has CM (distance based)
            if(statistics.name().contains("CM")) {
                distance += player.getStatistic(statistics);
            }
        }

        return (int)Math.round((double) distance / 100);
    }

    public void createPlayerStats(ForceItemPlayer forceItemPlayer) {
        if(this.playerExists(forceItemPlayer.player().getName())) return;

        ForceItemPlayerStats forceItemPlayerStats = new ForceItemPlayerStats(forceItemPlayer.player().getName(), 0, 0.0, 0, 0, 0, 0, 0, new ArrayList<>());

        this.playerStats.put(forceItemPlayer.player().getName(), forceItemPlayerStats);
        this.saveStats();
    }

}
