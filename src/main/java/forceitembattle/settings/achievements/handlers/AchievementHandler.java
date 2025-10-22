package forceitembattle.settings.achievements.handlers;

import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * Each handler is responsible for one type of achievement logic
 * AND declares which trigger it responds to
 */
public interface AchievementHandler<P extends ProgressTracker> {

    /**
     * Which trigger does this handler respond to?
     */
    Trigger getTrigger();

    /**
     * Check if achievement condition is met
     */
    boolean check(Event event, P progress, ForceItemPlayer forceItemPlayer);

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