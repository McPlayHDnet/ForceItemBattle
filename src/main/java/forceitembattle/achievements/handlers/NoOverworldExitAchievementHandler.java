package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class NoOverworldExitAchievementHandler extends TallyAchievementHandler {

    @Override
    public Trigger getTrigger() {
        return Trigger.VISIT;
    }

    /** Entering the nether or the end is what counts as leaving the overworld. */
    @Override
    protected boolean matches(Event event, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        return event instanceof PlayerChangedWorldEvent worldEvent
                && !Dimension.isOverworld(worldEvent.getPlayer());
    }

    @Override
    public boolean isTeamEligible() {
        return true;
    }
}
