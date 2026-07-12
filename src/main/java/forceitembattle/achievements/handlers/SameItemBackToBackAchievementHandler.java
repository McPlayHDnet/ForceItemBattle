package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.SameItemBackToBackAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

/**
 * "Get the same item as a back-to-back {@code targetAmount} times in a row."
 *
 * <p>Obtaining an item leaves it in the player's inventory, so if it keeps getting
 * reassigned it keeps auto-completing. With targetAmount=2 the qualifying sequence is
 * Egg &rarr; Egg (b2b) &rarr; Egg (b2b): the first Egg only has to be obtained, and it
 * makes no difference whether that came from a joker skip or a normal find — it is
 * never inspected. Only the back-to-backs are counted.
 *
 * <p>Any skip or non-back-to-back find breaks the run, as does a back-to-back landing
 * on a different material (which starts a fresh run for that material).
 *
 * <p>Distinct from {@link RepeatItemAchievementHandler}, which counts how often an item
 * is assigned across a whole round without requiring the assignments to be consecutive
 * or to be back-to-backs.
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
    public boolean check(Event event, SameItemBackToBackAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
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
