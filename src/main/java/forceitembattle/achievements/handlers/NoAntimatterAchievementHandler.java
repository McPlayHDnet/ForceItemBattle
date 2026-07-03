package forceitembattle.achievements.handlers;

import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class NoAntimatterAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    @Override
    public Trigger getTrigger() {
        return Trigger.ANTIMATTER_TELEPORTER;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof AntimatterTeleporterUseEvent) {
            progress.count++;
        }
        return false; // evaluated at game end
    }

    @Override
    public SimpleAchievementProgress createProgress() {
        return new SimpleAchievementProgress();
    }

    @Override
    public boolean isTeamEligible() {
        return true;
    }
}