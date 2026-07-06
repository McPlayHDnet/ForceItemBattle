package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class WheelOfFortuneAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.WHEEL_OF_FORTUNE;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof WheelOfFortuneWinEvent win)) {
            return false;
        }
        return win.getWonItem() == forceItemPlayer.getCurrentMaterial();
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
