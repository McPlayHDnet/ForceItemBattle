package forceitembattle.settings.achievements.handlers;

import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handler for death-counter achievements (Chicot - no deaths)
 */
public class DeathCounterHandler implements AchievementHandler<SimpleProgress> {

    private final int maxDeaths;

    public DeathCounterHandler(int maxDeaths) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof PlayerDeathEvent) {
            progress.deathCount++;
        }
        // Never triggers during the game - checked at end
        return false;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}