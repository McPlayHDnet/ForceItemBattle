package forceitembattle.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class NoBackToBackHandler implements AchievementHandler<SimpleProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.BACK_TO_BACK;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof FoundItemEvent foundEvent && foundEvent.isBackToBack()) {
            progress.count++;
        }
        return false; // evaluated at game end, never mid-game
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}