package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.CustomItemSpec;
import forceitembattle.achievements.Trigger;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class EatingAchievementHandler extends CountingAchievementHandler {

    /** Null means any consumable counts. */
    private final CustomItemSpec requiredItem;

    public EatingAchievementHandler(int targetAmount, CustomItemSpec requiredItem) {
        super(targetAmount);
        this.requiredItem = requiredItem;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.EATING;
    }

    @Override
    protected boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        return event instanceof PlayerItemConsumeEvent consumeEvent
                && (requiredItem == null || requiredItem.matches(consumeEvent.getItem()));
    }
}
