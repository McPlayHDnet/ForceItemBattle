package forceitembattle.model.stats;

/**
 * The player-scoped stat row: the figures that are neither solo nor team, but the player's across
 * both.
 *
 * <p>One field, because one field is all any caller reads — {@code GlobalStat.HIGHEST_WIN_STREAK}
 * is the only thing that ever touched {@code FibPlayerStatsDto}. Widen it when a second caller
 * needs a second figure, not before.
 */
public record GlobalPlayerStats(long highestWinStreak) {
}
