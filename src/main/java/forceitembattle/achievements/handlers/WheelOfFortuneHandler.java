package forceitembattle.achievements.handlers;

import forceitembattle.achievements.Trigger;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class WheelOfFortuneHandler implements AchievementHandler<SimpleProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.WHEEL_OF_FORTUNE;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof WheelOfFortuneWinEvent win)) {
            return false;
        }
        return win.getWonItem() == forceItemPlayer.currentMaterial();
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}