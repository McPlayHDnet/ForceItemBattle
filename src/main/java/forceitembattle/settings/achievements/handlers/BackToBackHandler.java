package forceitembattle.settings.achievements.handlers;

import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.event.Event;

public class BackToBackHandler implements AchievementHandler<BackToBackProgress> {

    private final int targetAmount;
    private final boolean requireSameItem;
    private final boolean requireSkippedThenGot;

    public BackToBackHandler(int targetAmount, boolean requireSameItem, boolean requireSkippedThenGot) {
        // VALIDATION: targetAmount must be positive
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1, got: " + targetAmount);
        }

        // VALIDATION: Can't have both requireSameItem and requireSkippedThenGot
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
    public boolean check(Event event, BackToBackProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        Material currentItem = foundEvent.getFoundItem().getType();

        // Track skipped items
        if (foundEvent.isSkipped()) {
            progress.lastSkippedItem = currentItem;
            // CRITICAL FIX: Reset b2b streak when player skips
            progress.b2bCount = 0;
            progress.lastItemType = null;
            return false;
        }

        // ACCIDENTAL GENIUS - skip then get same item via b2b
        if (requireSkippedThenGot) {
            if (foundEvent.isBackToBack() &&
                    foundEvent.isPreviousItemWasSkipped() &&
                    progress.lastSkippedItem != null &&
                    progress.lastSkippedItem == currentItem) {
                return true;
            }
            return false;
        }

        // For all other b2b achievements: must be a b2b event
        if (!foundEvent.isBackToBack()) {
            // Reset streak when getting a non-b2b item
            progress.b2bCount = 0;
            progress.lastItemType = currentItem;
            return false;
        }

        // It's a B2B event - increment the streak
        progress.b2bCount++;

        // DÉJÀ VU - same item b2b
        if (requireSameItem) {
            boolean matches = progress.lastItemType == currentItem;
            progress.lastItemType = currentItem;
            return matches && progress.b2bCount >= targetAmount;
        }

        // Regular b2b counter - check if streak matches target
        // targetAmount=1 → BACK_TO_BACK (1 b2b)
        // targetAmount=2 → DOUBLE_TROUBLE (2 b2b in a row)
        // targetAmount=3 → OH_BABY_A_TRIPLE (3 b2b in a row)
        progress.lastItemType = currentItem;
        return progress.b2bCount >= targetAmount;
    }

    @Override
    public BackToBackProgress createProgress() {
        return new BackToBackProgress();
    }
}