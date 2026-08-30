package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

public class WheelOfFortuneUsesAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;

    public WheelOfFortuneUsesAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.WHEEL_OF_FORTUNE;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof WheelOfFortuneWinEvent)) {
            return false;
        }
        progress.count++;
        return progress.count >= targetAmount;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
