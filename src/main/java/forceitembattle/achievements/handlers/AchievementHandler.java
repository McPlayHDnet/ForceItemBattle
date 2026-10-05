package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * Handlers are stateless strategies on the {@code Achievements} enum, so collaborators are passed
 * into {@link #check} rather than the constructor, which would depend on class-load order.
 */
public interface AchievementHandler<P> {

    Trigger getTrigger();

    boolean check(Event event, P progress, ForceItemPlayer forceItemPlayer, AchievementWorld world);

    P createProgress();

    default boolean isTeamEligible() {
        return getTrigger().isAchieveableInTeams();
    }

    /** Whether progress stays per-player in a team game instead of one tracker shared by the team. */
    default boolean isPlayerBased() {
        return false;
    }
}
