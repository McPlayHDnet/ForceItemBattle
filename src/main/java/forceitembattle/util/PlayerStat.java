package forceitembattle.util;

public enum PlayerStat {

    TOTAL_ITEMS(true),
    TRAVELLED(true),
    GAMES_PLAYED(false),
    GAMES_WON(true),
    HIGHEST_SCORE(true),
    BACK_TO_BACK_STREAK(true),
    WIN_STREAK(true);

    private final boolean isInLeaderboard;

    PlayerStat(boolean isInLeaderboard) {
        this.isInLeaderboard = isInLeaderboard;
    }

    public boolean isInLeaderboard() {
        return isInLeaderboard;
    }
}
