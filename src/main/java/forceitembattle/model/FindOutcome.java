package forceitembattle.model;

/**
 * What a {@link Find} is worth: the four decisions that used to sit as inline guards spread through
 * {@code FoundItemListener.onFoundItem}.
 *
 * <p>They are gathered here because each one is a rule rather than an effect, each was easy to read
 * past, and every one of them is silently wrong when it is wrong — a find that quietly fails to
 * score, or a streak that survives a skip, looks exactly like a find that worked.
 *
 * <p>Pure: no Bukkit, no plugin, no side effects. Computed once at the top of the pipeline, before
 * anything advances, because two of the four read state that advancing destroys.
 */
public record FindOutcome(boolean announces,
                          boolean scores,
                          boolean recordsStats,
                          long timeSpentMs,
                          int newItemStreak) {

    public static FindOutcome of(Find find, GameContext context, long now) {
        ForceItemPlayer finder = find.finder();

        // A back-to-back is announced by BackToBackManager, with the odds attached. Announcing it
        // here as well would print the plain line under the interesting one.
        boolean announces = !find.backToBack();

        // Run mode is a race for the first find, so a skip there is not worth a point. Everywhere
        // else a skip still scores -- spending a joker is how you buy the point.
        boolean scores = !context.runMode() || !find.skipped();

        boolean recordsStats = context.statsEnabled() && !context.runMode();

        // A back-to-back was never hunted, so it took no time. Otherwise measure from when the item
        // was handed out; a zero assignment stamp means it never was, which happens on the first
        // find of a round that started mid-flight.
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
