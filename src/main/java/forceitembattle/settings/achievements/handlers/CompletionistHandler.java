package forceitembattle.settings.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.settings.achievements.Achievements;
import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * Handler for the Completionist++ achievement.
 * Triggers when a player has earned all other achievements.
 */
public class CompletionistHandler implements AchievementHandler<SimpleProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.ACHIEVEMENT;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof PlayerGrantAchievementEvent)) {
            return false;
        }

        var storage = ForceItemBattle.getInstance().getAchievementManager().getAchievementStorage();
        int completed = storage.getPlayerAchievements(forceItemPlayer.player().getUniqueId()).size();

        // -1 because we don't count Completionist itself
        return completed >= (Achievements.values().length - 1);
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}