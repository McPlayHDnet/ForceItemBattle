package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class BackToBackCountAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;

    public BackToBackCountAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.BACK_TO_BACK;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin ) {
        if (event instanceof FoundItemEvent foundEvent && foundEvent.isBackToBack()) {
            progress.count++;
            return progress.count >= targetAmount;
        }
        return false;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}