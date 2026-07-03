package forceitembattle.achievements.handlers;

import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handler for death-counter achievements (Chicot - no deaths)
 */
public class DeathCounterAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int maxDeaths;

    public DeathCounterAchievementHandler(int maxDeaths) {
        if (maxDeaths < 0) {
            throw new IllegalArgumentException("maxDeaths cannot be negative");
        }
        this.maxDeaths = maxDeaths;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.DYING;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof PlayerDeathEvent) {
            progress.deathCount++;
        }
        // Never triggers during the game - checked at end
        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}