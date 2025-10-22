package forceitembattle.settings.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.settings.achievements.Trigger;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.event.Event;

public class TimeBasedHandler implements AchievementHandler<TimeProgress> {

    private final int targetAmount;
    private final long withinSeconds;
    private final long timeFrameSeconds;
    private final long skipAfterSeconds;
    private final int closeCallSeconds;
    private final boolean noSkip;
    private final boolean firstPlayer;
    private final boolean playerBased;

    public TimeBasedHandler(int targetAmount, long withinSeconds, long timeFrameSeconds,
                            long skipAfterSeconds, int closeCallSeconds, boolean noSkip,
                            boolean firstPlayer, boolean playerBased) {

        // VALIDATION: Only one time constraint should be set
        int constraintCount = 0;
        if (withinSeconds > 0) constraintCount++;
        if (timeFrameSeconds > 0) constraintCount++;
        if (skipAfterSeconds > 0) constraintCount++;
        if (closeCallSeconds > 0) constraintCount++;
        if (firstPlayer) constraintCount++;

        if (constraintCount != 1) {
            throw new IllegalArgumentException(
                    "TimeBasedHandler must have exactly ONE time constraint! " +
                            "(withinSeconds=" + withinSeconds + ", timeFrameSeconds=" + timeFrameSeconds +
                            ", skipAfterSeconds=" + skipAfterSeconds + ", closeCallSeconds=" + closeCallSeconds +
                            ", firstPlayer=" + firstPlayer + ")"
            );
        }

        // VALIDATION: targetAmount must be positive
        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1, got: " + targetAmount);
        }

        // VALIDATION: noSkip only makes sense with certain time constraints
        if (noSkip && (skipAfterSeconds > 0 || firstPlayer)) {
            throw new IllegalArgumentException(
                    "noSkip=true doesn't make sense with skipAfterSeconds or firstPlayer"
            );
        }

        this.targetAmount = targetAmount;
        this.withinSeconds = withinSeconds;
        this.timeFrameSeconds = timeFrameSeconds;
        this.skipAfterSeconds = skipAfterSeconds;
        this.closeCallSeconds = closeCallSeconds;
        this.noSkip = noSkip;
        this.firstPlayer = firstPlayer;
        this.playerBased = playerBased;
    }

    @Override
    public Trigger getTrigger() {
        return Trigger.OBTAIN_ITEM_IN_TIME;
    }

    @Override
    public boolean check(Event event, TimeProgress progress, ForceItemPlayer forceItemPlayer) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedGameTime = (currentTime - progress.gameStartTime) / 1000;
        long elapsedItemTime = (currentTime - progress.lastItemTime) / 1000;

        // Update item received time for non-b2b, non-skip items
        if (!foundEvent.isBackToBack() && !foundEvent.isSkipped()) {
            progress.itemReceivedTime = currentTime;
        }

        // Track if player has skipped
        if (foundEvent.isSkipped()) {
            progress.hasSkipped = true;
        }

        // PROCRASTINATOR - special case: ONLY triggers on skip events
        if (skipAfterSeconds > 0) {
            if (!foundEvent.isSkipped()) {
                return false; // Not a skip, so can't be procrastinator
            }
            long timeSinceReceived = (currentTime - progress.itemReceivedTime) / 1000;
            return timeSinceReceived >= skipAfterSeconds;
        }

        // ALL OTHER TIME-BASED ACHIEVEMENTS: Skip events don't count
        if (foundEvent.isSkipped()) {
            return false;
        }

        // If noSkip is true and player has skipped, they can't get this achievement
        if (noSkip && progress.hasSkipped) {
            return false;
        }

        // EARLY BIRD - first player to collect (non-b2b, non-skip)
        if (firstPlayer) {
            if (foundEvent.isBackToBack()) {
                return false; // B2B doesn't count as "first item"
            }
            if (!progress.firstItemCollected) {
                ForceItemBattle fib = ForceItemBattle.getInstance();
                boolean isFirstGlobally = fib.getGamemanager().forceItemPlayerMap().values().stream()
                        .filter(p -> !p.isSpectator())
                        .allMatch(p -> p.foundItems().isEmpty() ||
                                p.player().getUniqueId().equals(forceItemPlayer.player().getUniqueId()));

                if (isFirstGlobally) {
                    progress.firstItemCollected = true;
                    return true;
                }
            }
            return false;
        }

        // CLOSE CALL - within last X seconds of round
        if (closeCallSeconds > 0) {
            ForceItemBattle fib = ForceItemBattle.getInstance();
            long totalRoundTime = fib.getGamemanager().getGameDuration();
            long remainingTime = totalRoundTime - elapsedGameTime;
            return remainingTime <= closeCallSeconds;
        }

        // Within X seconds from game start
        if (withinSeconds > 0) {
            if (elapsedGameTime > withinSeconds) {
                return false;
            }
            progress.count++;
            progress.lastItemTime = currentTime;
            return progress.count >= targetAmount;
        }

        // Took at least X seconds to find
        if (timeFrameSeconds > 0) {
            if (elapsedItemTime < timeFrameSeconds) {
                progress.count = 0;
                progress.lastItemTime = currentTime;
                return false;
            }
            progress.count++;
            progress.lastItemTime = currentTime;
            return progress.count >= targetAmount;
        }

        return false;
    }

    @Override
    public TimeProgress createProgress() {
        return new TimeProgress();
    }

    @Override
    public boolean isPlayerBased() {
        return playerBased;
    }
}