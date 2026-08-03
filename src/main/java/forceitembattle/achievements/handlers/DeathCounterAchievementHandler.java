package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Counts deaths only; CHICOT is awarded from the count by checkGameEndAchievements. */
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
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (event instanceof PlayerDeathEvent) {
            progress.deathCount++;
        }
        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
