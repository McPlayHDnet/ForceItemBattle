package forceitembattle.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class BackToBackCountHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;

    public BackToBackCountHandler(int targetAmount) {
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
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof FoundItemEvent foundEvent && foundEvent.isBackToBack()) {
            progress.count++;
            return progress.count >= targetAmount;
        }
        return false;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}