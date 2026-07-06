package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
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

    /**
     * Which trigger does this handler respond to?
     */
    Trigger getTrigger();

    /**
     * Check if achievement condition is met
     */
    boolean check(Event event, P progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin);

    /**
     * Create progress tracker
     */
    P createProgress();

    /**
     * Can this be earned by teams?
     */
    default boolean isTeamEligible() {
        return getTrigger().isAchieveableInTeams();
    }

    /**
     * Is this player-specific?
     */
    default boolean isPlayerBased() {
        return false;
    }
}
