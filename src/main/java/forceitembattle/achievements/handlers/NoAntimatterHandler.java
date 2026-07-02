package forceitembattle.achievements.handlers;

import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class NoAntimatterHandler implements AchievementHandler<SimpleProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.ANTIMATTER_TELEPORTER;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof AntimatterTeleporterUseEvent) {
            progress.count++;
        }
        return false; // evaluated at game end
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }

    @Override
    public boolean isTeamEligible() {
        return true;
    }
}