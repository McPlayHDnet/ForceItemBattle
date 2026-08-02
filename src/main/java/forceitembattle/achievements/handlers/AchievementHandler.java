package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.AchievementProgressTracker;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * Each handler is responsible for one type of achievement logic
 * AND declares which trigger it responds to.
 *
 * <p>Handlers are stateless strategies held on the {@code Achievements} enum, so
 * they can't be given the plugin at construction (that would depend on class-load
 * order). Collaborators they need are passed into {@link #check} at call time by
 * the {@code AchievementManager}, which owns the plugin reference.
 */
public interface AchievementHandler<P extends AchievementProgressTracker> {

    Trigger getTrigger();

    boolean check(Event event, P progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin);

    P createProgress();

    default boolean isTeamEligible() {
        return getTrigger().isAchieveableInTeams();
    }

    /**
     * Whether progress stays per-player even in a team game. Team-eligible handlers otherwise
     * share one tracker across the team, so either member's actions advance it.
     */
    default boolean isPlayerBased() {
        return false;
    }
}
