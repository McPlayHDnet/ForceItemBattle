package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.BackToBackAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class BackToBackAchievementHandler implements AchievementHandler<BackToBackAchievementProgress> {

    private final int targetAmount;
    private final boolean requireSameItem;
    private final boolean requireSkippedThenGot;

    public BackToBackAchievementHandler(int targetAmount, boolean requireSameItem, boolean requireSkippedThenGot) {
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1, got: " + targetAmount);
        }

        // The preceding item is either skipped or normally obtained, not both.
        if (requireSameItem && requireSkippedThenGot) {
            throw new IllegalArgumentException(
                    "Cannot have both requireSameItem and requireSkippedThenGot - they are mutually exclusive"
            );
        }

        this.targetAmount = targetAmount;
        this.requireSameItem = requireSameItem;
        this.requireSkippedThenGot = requireSkippedThenGot;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.BACK_TO_BACK;
    }

    @Override
    public boolean check(Event event, BackToBackAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        Material currentItem = foundEvent.getFoundItem().getType();

        // Snapshot the previous event, then record this one for the next call. Doing this
        // up front means the one-event lookback holds no matter which branch we take.
        Material previousItem = progress.previousItem;
        boolean previousWasSkip = progress.previousWasSkip;
        progress.previousItem = currentItem;
        progress.previousWasSkip = foundEvent.isSkipped();

        // A skip is never itself an achievement-granting find; it only breaks the streak
        // and becomes the "previous item" for the next event.
        if (foundEvent.isSkipped()) {
            progress.b2bCount = 0;
            return false;
        }

        // DEJA_VU / ACCIDENTAL_GENIUS: a back-to-back on the same material as the item
        // obtained immediately before, obtained the required way.
        if (requireSameItem || requireSkippedThenGot) {
            if (!foundEvent.isBackToBack() || previousItem == null || previousItem != currentItem) {
                return false;
            }
            return previousWasSkip == requireSkippedThenGot;
        }

        // Streak counters: a non-b2b item breaks the run.
        if (!foundEvent.isBackToBack()) {
            progress.b2bCount = 0;
            return false;
        }

        // targetAmount=1 → BACK_TO_BACK, 2 → DOUBLE_TROUBLE, 3 → OH_BABY_A_TRIPLE, ...
        progress.b2bCount++;
        return progress.b2bCount >= targetAmount;
    }

    @Override
    public BackToBackAchievementProgress createProgress() {
        return new BackToBackAchievementProgress();
    }
}
