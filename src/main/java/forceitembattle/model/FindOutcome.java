package forceitembattle.model;

/**
 * What a {@link Find} is worth. Pure: no Bukkit, no plugin, no side effects. Computed once at the top
 * of the pipeline, before anything advances, because two of the four decisions read state that
 * advancing destroys.
 */
public record FindOutcome(boolean announces,
                          boolean scores,
                          boolean recordsStats,
                          long timeSpentMs,
                          int newItemStreak) {

    public static FindOutcome of(Find find, GameContext context, long now) {
        ForceItemPlayer finder = find.finder();

        // BackToBackManager announces those itself, with the odds attached.
        boolean announces = !find.backToBack();

        // Run mode is a race for the first find, so a skip there is not worth a point. Everywhere
        // else spending a joker is how you buy the point.
        boolean scores = !context.runMode() || !find.skipped();

        boolean recordsStats = context.statsEnabled() && !context.runMode();

        // A back-to-back was never hunted, so it took no time. A zero assignment stamp means the item
        // never was handed out, which happens on the first find of a round that started mid-flight.
        long timeSpentMs = 0L;
        if (!find.backToBack()) {
            long assignedAt = finder.activeItemAssignedAt();
            if (assignedAt > 0) {
                timeSpentMs = now - assignedAt;
            }
        }

        // The item streak counts every item obtained, back-to-backs included; only a skip breaks it.
        int newItemStreak = find.skipped() ? 0 : finder.itemStreak() + 1;

        return new FindOutcome(announces, scores, recordsStats, timeSpentMs, newItemStreak);
    }
}
