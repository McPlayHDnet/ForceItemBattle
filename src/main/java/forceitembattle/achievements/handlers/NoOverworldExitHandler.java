package forceitembattle.achievements.handlers;

import forceitembattle.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class NoOverworldExitHandler implements AchievementHandler<SimpleProgress> {

    private static final String OVERWORLD = "world";

    @Override
    public Trigger getTrigger() {
        return Trigger.VISIT;
    }

    @Override
    public boolean check(Event event, SimpleProgress progress, ForceItemPlayer forceItemPlayer) {
        if (event instanceof PlayerChangedWorldEvent worldEvent
                && !worldEvent.getPlayer().getWorld().getName().equals(OVERWORLD)) {
            progress.count++; // entered the nether/end = left the overworld
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