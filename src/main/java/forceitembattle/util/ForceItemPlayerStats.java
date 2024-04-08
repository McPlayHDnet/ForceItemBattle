package forceitembattle.util;

import lombok.Setter;

@Setter
public class ForceItemPlayerStats {

    private String userName;
    private int totalItemsFound, gamesPlayed, gamesWon, highestScore;
    private double travelled;

    public ForceItemPlayerStats(String userName, int totalItemsFound, double travelled, int gamesPlayed, int gamesWon, int highestScore) {
        this.userName = userName;
        this.totalItemsFound = totalItemsFound;
        this.travelled = travelled;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.highestScore = highestScore;
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

}


