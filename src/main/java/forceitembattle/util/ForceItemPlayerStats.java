package forceitembattle.util;

import forceitembattle.manager.stats.SeasonalStats;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.*;

@Setter
@Getter
public class ForceItemPlayerStats {

    private String userName;
    private Map<String, SeasonalStats> seasonalStatsMap;
    private List<String> achievementsDone;

    public ForceItemPlayerStats(String userName) {
        this.userName = userName;
        this.seasonalStatsMap = new HashMap<>();
        this.achievementsDone = new ArrayList<>();
    }

    public SeasonalStats getSeasonStats(String season) {
        return this.seasonalStatsMap.computeIfAbsent(season, k -> new SeasonalStats());
    }

    public List<String> getSeasons() {
        return new ArrayList<>(this.seasonalStatsMap.keySet());
    }

    public void setSeasonStats(String season, SeasonalStats seasonalStats) {
        this.seasonalStatsMap.put(season, seasonalStats);
    }

    public boolean hasSeason(String season) {
        return this.seasonalStatsMap.containsKey(season);
    }

}


