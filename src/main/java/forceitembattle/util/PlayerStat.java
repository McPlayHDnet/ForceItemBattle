package forceitembattle.util;

import lombok.Getter;

@Getter
public enum PlayerStat {

    TOTAL_ITEMS("total_items", true),
    TRAVELLED("travelled", true),
    GAMES_PLAYED("games_played", false),
    GAMES_WON("games_won", true),
    HIGHEST_SCORE("highest_score", true),
    BACK_TO_BACK_STREAK("back_to_back_streak", true),
    WIN_STREAK("win_streak", true);

    private final String statKey;
    private final boolean isInLeaderboard;

    PlayerStat(String statKey, boolean isInLeaderboard) {
        this.statKey = statKey;
        this.isInLeaderboard = isInLeaderboard;
    }

    public boolean isInLeaderboard() {
        return isInLeaderboard;
    }
}
