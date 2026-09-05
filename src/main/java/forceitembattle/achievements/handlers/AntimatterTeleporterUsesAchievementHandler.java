package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;

public class AntimatterTeleporterUsesAchievementHandler extends CountingAchievementHandler {

    public AntimatterTeleporterUsesAchievementHandler(int targetAmount) {
        super(targetAmount);
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.ANTIMATTER_TELEPORTER;
    }

    @Override
    protected boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        return event instanceof AntimatterTeleporterUseEvent teleporterEvent && teleporterEvent.isNewTeleporter();
    }
}
