package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SimpleAchievementProgress;
import forceitembattle.model.Dimension;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class NoOverworldExitAchievementHandler implements AchievementHandler<SimpleAchievementProgress> {

    private static final String OVERWORLD = "world";

    @Override
    public Trigger getTrigger() {
        return Trigger.VISIT;
    }

    @Override
    public boolean check(Event event, SimpleAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (event instanceof PlayerChangedWorldEvent worldEvent && !Dimension.isOverworld(worldEvent.getPlayer())) {
            progress.count++; // entered the nether/end = left the overworld
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
