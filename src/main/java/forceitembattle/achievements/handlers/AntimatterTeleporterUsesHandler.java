package forceitembattle.achievements.handlers;

import forceitembattle.achievements.Trigger;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class AntimatterTeleporterUsesHandler implements AchievementHandler<SimpleProgress> {

    private final int targetAmount;

    public AntimatterTeleporterUsesHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1");
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.ANTIMATTER_TELEPORTER;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof AntimatterTeleporterUseEvent teleporterEvent) || !teleporterEvent.isNewTeleporter()) {
            return false;
        }
        progress.count++;
        return progress.count >= targetAmount;
    }

    @Override
    public SimpleProgress createProgress() {
        return new SimpleProgress();
    }
}