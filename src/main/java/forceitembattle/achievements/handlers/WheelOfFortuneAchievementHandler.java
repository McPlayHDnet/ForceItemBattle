package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

public class WheelOfFortuneAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.WHEEL_OF_FORTUNE;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof WheelOfFortuneWinEvent win)) {
            return false;
        }
        return win.getWonItem() == forceItemPlayer.activeMaterial();
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
