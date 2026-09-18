package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementManager;
import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * "Finish the game without doing this." The subclass says only what counts as doing it; the tally is
 * read at game end by {@code AchievementManager.checkGameEndAchievements}, which unlocks when the
 * count is still zero.
 *
 * <p>{@link #check} is final and always returns false, which is the whole point: these can never
 * complete mid-game, because until the game is over there is always another chance to break the
 * streak. That rule used to live as a {@code // evaluated at game end} comment repeated next to
 * three identical {@code return false} statements; here it is the type.
 */
public abstract class TallyAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    /** Whether this event is an occurrence of the thing that spoils the achievement. */
    protected abstract boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world);

    @Override
    public final boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (matches(event, forceItemPlayer, world)) {
            progress.count++;
        }
        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
