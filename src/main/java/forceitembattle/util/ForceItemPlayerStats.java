package forceitembattle.util;

import forceitembattle.settings.achievements.Achievement;
import lombok.Setter;

import java.util.List;

@Setter
public class ForceItemPlayerStats {

    private String userName;
    private int totalItemsFound, gamesPlayed, gamesWon, highestScore, back2backStreak, winStreak;
    private double travelled;
    private List<String> achievementsDone;

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
}


