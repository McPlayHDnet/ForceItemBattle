package forceitembattle.util;

import lombok.Setter;
import org.bukkit.Material;

import java.util.*;

@Setter
public class ForceItemPlayerStats {

    private String userName;
    private int totalItemsFound, gamesPlayed, gamesWon, highestScore, back2backStreak, winStreak;
    private double travelled;
    private List<String> achievementsDone;

    private SortedMap<Material, Integer> mostFoundItems;

    public ForceItemPlayerStats(String userName, int totalItemsFound, double travelled, int gamesPlayed, int gamesWon, int highestScore, int back2backStreak, int winStreak, List<String> achievementsDone) {
        this.userName = userName;
        this.totalItemsFound = totalItemsFound;
        this.travelled = travelled;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.highestScore = highestScore;
        this.back2backStreak = back2backStreak;
        this.winStreak = winStreak;
        this.achievementsDone = achievementsDone;

        this.mostFoundItems = new TreeMap<>(Comparator.reverseOrder());
    }

    public String userName() {
        return userName;
    }

    public int totalItemsFound() {
        return totalItemsFound;
    }

    public int gamesPlayed() {
        return gamesPlayed;
    }

    public int gamesWon() {
        return gamesWon;
    }

    public int highestScore() {
        return highestScore;
    }

    public double travelled() {
        return travelled;
    }

    public int back2backStreak() {
        return back2backStreak;
    }

    public int winStreak() {
        return winStreak;
    }

    public List<String> achievementsDone() {
        return achievementsDone;
    }

    public SortedMap<Material, Integer> mostFoundItems() {
        if (this.mostFoundItems == null) {
            this.mostFoundItems = new TreeMap<>(Comparator.reverseOrder());
        }
        return this.mostFoundItems;
    }

    public void addFoundItem(ForceItemPlayer forceItemPlayer) {
        SortedMap<Material, Integer> mostFoundItems = this.mostFoundItems();

        Map<Material, Integer> materialCounts = new HashMap<>();
        for (ForceItem forceItem : forceItemPlayer.foundItems()) {
            Material material = forceItem.material();
            materialCounts.put(material, materialCounts.getOrDefault(material, 0) + 1);
        }

        List<Map.Entry<Material, Integer>> sortedEntries = new ArrayList<>(materialCounts.entrySet());
        sortedEntries.sort(Map.Entry.<Material, Integer>comparingByValue().reversed());

        for (Map.Entry<Material, Integer> entry : sortedEntries.subList(0, Math.min(3, sortedEntries.size()))) {
            mostFoundItems.put(entry.getKey(), entry.getValue());
        }

        this.mostFoundItems = new TreeMap<>(mostFoundItems);
    }
}


