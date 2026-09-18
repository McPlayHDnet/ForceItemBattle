package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * "Do this N times." The subclass says only what counts as one occurrence; the counting, the target
 * comparison and the tracker are the same for every one of them.
 *
 * <p>{@link #check} is final on purpose. The count-then-compare order is what makes the last
 * occurrence the one that unlocks, and it was written out by hand in six handlers before this class
 * existed — a subclass that reimplemented it would only reintroduce the drift.
 *
 * <p>Contrast {@link TallyAchievementHandler}, which counts the same way but never completes
 * mid-game because its achievement is about the count being <em>zero</em> at the end.
 */
public abstract class CountingAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    protected final int targetAmount;

    protected CountingAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    /** Whether this event is one occurrence of the thing being counted. */
    protected abstract boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world);

    @Override
    public final boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!matches(event, forceItemPlayer, world)) {
            return false;
        }
        progress.count++;
        return progress.count >= targetAmount;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
