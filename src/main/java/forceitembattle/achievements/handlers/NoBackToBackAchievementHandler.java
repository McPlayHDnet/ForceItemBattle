package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class NoBackToBackAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.BACK_TO_BACK;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (event instanceof FoundItemEvent foundEvent && foundEvent.isBackToBack()) {
            progress.count++;
        }
        return false; // evaluated at game end, never mid-game
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}