package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForceItemPlayerStats {

    private String userName;
    private int totalItemsFound;
    private int gamesPlayed;
    private int gamesWon;
    private int highestScore;
    private double travelled;

    public ForceItemPlayerStats(String userName, int totalItemsFound, double travelled, int gamesPlayed, int gamesWon, int highestScore) {
        this.userName = userName;
        this.totalItemsFound = totalItemsFound;
        this.travelled = travelled;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.highestScore = highestScore;
    }

}


