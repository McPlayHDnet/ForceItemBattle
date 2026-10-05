package forceitembattle.model.stats;

/** One field because one is all any caller reads; widen it when a second caller needs more. */
public record GlobalPlayerStats(long highestWinStreak) {
}
