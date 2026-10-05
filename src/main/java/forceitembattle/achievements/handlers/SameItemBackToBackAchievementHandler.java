package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SameItemBackToBackAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

/**
 * Same item as a back-to-back N times in a row. The first assignment only has to be obtained; any
 * skip, normal find or back-to-back on a different material breaks the run.
 */
public class SameItemBackToBackAchievementHandler implements AchievementHandler<SameItemBackToBackAchievementProgress> {

    private final int targetAmount;

    public SameItemBackToBackAchievementHandler(int targetAmount) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1, got: " + targetAmount);
        }
        this.targetAmount = targetAmount;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.BACK_TO_BACK;
    }

    @Override
    public boolean check(Event event, SameItemBackToBackAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        // A skip or an ordinary find breaks the chain of back-to-backs.
        if (foundEvent.isSkipped() || !foundEvent.isBackToBack()) {
            progress.lastBackToBackItem = null;
            progress.sameItemStreak = 0;
            return false;
        }

        Material currentItem = foundEvent.getFoundItem().getType();

        if (progress.lastBackToBackItem == currentItem) {
            progress.sameItemStreak++;
        } else {
            // A back-to-back on a different material starts a fresh run for that material.
            progress.lastBackToBackItem = currentItem;
            progress.sameItemStreak = 1;
        }

        return progress.sameItemStreak >= targetAmount;
    }

    @Override
    public SameItemBackToBackAchievementProgress createProgress() {
        return new SameItemBackToBackAchievementProgress();
    }
}
