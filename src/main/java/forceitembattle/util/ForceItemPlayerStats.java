package forceitembattle.util;

public class ForceItemPlayerStats {

    private String userName;
    private int totalItemsFound, gamesPlayed, gamesWon, highestScore;

    public ForceItemPlayerStats(String userName, int totalItemsFound, int gamesPlayed, int gamesWon, int highestScore) {
        this.userName = userName;
        this.totalItemsFound = totalItemsFound;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.highestScore = highestScore;
    }

    public String userName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int totalItemsFound() {
        return totalItemsFound;
    }

    public void setTotalItemsFound(int totalItemsFound) {
        this.totalItemsFound = totalItemsFound;
    }

    public int gamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int gamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int highestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }
}


