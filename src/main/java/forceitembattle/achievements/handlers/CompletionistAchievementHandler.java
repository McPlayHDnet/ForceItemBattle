package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Achievements;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.event.PlayerGrantAchievementEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

/**
 * Handler for the Completionist++ achievement.
 * Triggers when a player has earned all other achievements.
 */
public class CompletionistAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.ACHIEVEMENT;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof PlayerGrantAchievementEvent)) {
            return false;
        }

        var storage = plugin.getAchievementManager().getAchievementStorage();
        int completed = storage.getPlayerAchievements(forceItemPlayer.player().getUniqueId()).size();

        // -1 because we don't count Completionist itself
        return completed >= (Achievements.values().length - 1);
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
