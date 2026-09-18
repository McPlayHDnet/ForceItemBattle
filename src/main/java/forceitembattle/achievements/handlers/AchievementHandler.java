package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.AchievementProgressTracker;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * One type of achievement logic, plus the trigger it responds to.
 *
 * <p>Handlers are stateless strategies held on the {@code Achievements} enum, so they cannot be given
 * collaborators at construction — that would depend on class-load order. What they need is passed
 * into {@link #check} at call time.
 *
 * <p><b>{@link AchievementWorld}, not the plugin.</b> Taking the plugin makes every rule's real
 * interface every manager, and this parameter is what decides whether this package can be tested
 * without a running server. A rule that needs something new widens the world by one named question.
 */
public interface AchievementHandler<P extends AchievementProgressTracker> {

    Trigger getTrigger();

    boolean check(Event event, P progress, ForceItemPlayer forceItemPlayer, AchievementWorld world);

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
