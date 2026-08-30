package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.CustomItem;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class EatingAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private final int targetAmount;
    private final CustomItem requiredItem;

    public EatingAchievementHandler(int targetAmount, CustomItem requiredItem) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
        this.requiredItem = requiredItem;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.EATING;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof PlayerItemConsumeEvent consumeEvent)) {
            return false;
        }

        if (requiredItem != null && !requiredItem.matches(consumeEvent.getItem())) {
            return false;
        }

        progress.count++;
        return progress.count >= targetAmount;
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }
}
