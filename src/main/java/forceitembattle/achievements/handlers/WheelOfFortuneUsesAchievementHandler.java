package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.WheelOfFortuneWinEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

public class WheelOfFortuneUsesAchievementHandler extends CountingAchievementHandler {

    public WheelOfFortuneUsesAchievementHandler(int targetAmount) {
        super(targetAmount);
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.WHEEL_OF_FORTUNE;
    }

    @Override
    protected boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        return event instanceof WheelOfFortuneWinEvent;
    }
}
