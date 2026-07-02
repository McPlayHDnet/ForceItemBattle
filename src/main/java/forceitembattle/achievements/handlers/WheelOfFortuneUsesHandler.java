package forceitembattle.achievements.handlers;

import forceitembattle.achievements.Trigger;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class WheelOfFortuneUsesHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;

    public WheelOfFortuneUsesHandler(int targetAmount) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof WheelOfFortuneWinEvent)) {
            return false;
        }
        progress.count++;
        return progress.count >= targetAmount;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}