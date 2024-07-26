package forceitembattle.manager.stats;

import forceitembattle.util.PlayerStat;
import lombok.Getter;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class SeasonalStats {
    private double travelled;

    private GameStats totalItemsFound;
    private GameStats gamesPlayed;
    private GameStats gamesWon;
    private GameStats highestScore;
    private GameStats back2backStreak;
    private GameStats winStreak;

    public SeasonalStats() {
        this.totalItemsFound = new GameStats();
        this.gamesPlayed = new GameStats();
        this.gamesWon = new GameStats();
        this.highestScore = new GameStats();
        this.back2backStreak = new GameStats();
        this.winStreak = new GameStats();
        this.travelled = 0.0;
    }

    public void updateTeamStats(String playerName, int value, PlayerStat statType) {
        Map<String, TeamStat> teamStatsMap = switch (statType) {
            case GAMES_PLAYED -> gamesPlayed.getTeam();
            case GAMES_WON -> gamesWon.getTeam();
            case TOTAL_ITEMS -> totalItemsFound.getTeam();
            case HIGHEST_SCORE -> highestScore.getTeam();
            case BACK_TO_BACK_STREAK -> back2backStreak.getTeam();
            case WIN_STREAK -> winStreak.getTeam();
            default -> throw new IllegalArgumentException("Invalid stat type: " + statType);
        };

        TeamStat teamStat = teamStatsMap.computeIfAbsent(playerName, k -> new TeamStat(value, new SimpleDateFormat("dd/MM/yyyy").format(new Date())));
        teamStat.setValue((statType == PlayerStat.BACK_TO_BACK_STREAK || statType == PlayerStat.HIGHEST_SCORE || statType == PlayerStat.WIN_STREAK) ? value : teamStat.getValue() + value);
        teamStat.setLastUpdated(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
    }

    @Getter
    @Setter
    public static class GameStats {
        private int solo;
        private Map<String, TeamStat> team;

        public GameStats() {
            this.solo = 0;
            this.team = new HashMap<>();
        }
    }

    @Getter
    @Setter
    public static class TeamStat {
        private int value;
        private String lastUpdated;

        public TeamStat(int value, String lastUpdated) {
            this.value = value;
            this.lastUpdated = lastUpdated;
        }
    }
}
