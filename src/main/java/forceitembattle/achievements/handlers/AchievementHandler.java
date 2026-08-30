package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.AchievementProgressTracker;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * Each handler is responsible for one type of achievement logic
 * AND declares which trigger it responds to.
 *
 * <p>Handlers are stateless strategies held on the {@code Achievements} enum, so
 * they can't be given collaborators at construction (that would depend on class-load
 * order). What they need is passed into {@link #check} at call time by the
 * {@code AchievementManager}.
 *
 * <p><b>{@link AchievementWorld}, not the plugin.</b> This parameter used to be a
 * {@code ForceItemBattle}, which made every rule's real interface all 23 managers.
 * Seventeen of the 22 handlers never touched it; the other five called six methods
 * between them, and those six are what the world exposes. Keep it that way: a rule
 * that needs something new should widen the world by one named question, not reach
 * for the plugin again. The parameter is what decides whether this package can be
 * tested without a running server.
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
