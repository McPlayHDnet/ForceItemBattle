package forceitembattle.achievements.handlers;

import forceitembattle.achievements.AchievementListener;
import forceitembattle.achievements.AchievementWorld;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.TimeAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ScoreOwner;
import org.bukkit.event.Event;

public class TimeBasedAchievementHandler implements AchievementHandler<TimeAchievementProgress> {

    private final int targetAmount;
    private final long withinSeconds;
    private final long timeFrameSeconds;
    private final long skipAfterSeconds;
    private final int closeCallSeconds;
    private final boolean noSkip;
    private final boolean firstPlayer;
    private final boolean playerBased;

    public TimeBasedAchievementHandler(int targetAmount, long withinSeconds, long timeFrameSeconds,
                                       long skipAfterSeconds, int closeCallSeconds, boolean noSkip,
                                       boolean firstPlayer, boolean playerBased) {

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

        if (targetAmount < 1) {
            throw new IllegalArgumentException("targetAmount must be at least 1, got: " + targetAmount);
        }

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
    public boolean check(Event event, TimeAchievementProgress progress, ForceItemPlayer forceItemPlayer, AchievementWorld world) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        // Pause-aware game clock: the round clock only advances during MID_GAME, so anything
        // derived from it (rather than from wall time) stays correct across /pause and /resume.
        int secondsLeft = world.secondsLeft();
        long elapsedGameTime = world.elapsedSeconds();

        // Anchor per-item markers to the round start on first use. The first item
        // is assigned at game start, i.e. when secondsLeft == gameDuration.
        if (!progress.initialized) {
            progress.lastItemSecondsLeft = world.roundDuration();
            progress.itemReceivedSecondsLeft = world.roundDuration();
            progress.initialized = true;
        }

        long elapsedItemTime = progress.lastItemSecondsLeft - secondsLeft;

        if (!foundEvent.isBackToBack() && !foundEvent.isSkipped()) {
            progress.itemReceivedSecondsLeft = secondsLeft;
        }

        if (foundEvent.isSkipped()) {
            progress.hasSkipped = true;
        }

        // PROCRASTINATOR is the one achievement here that only triggers on skips.
        if (skipAfterSeconds > 0) {
            if (!foundEvent.isSkipped()) {
                return false;
            }
            long timeSinceReceived = progress.itemReceivedSecondsLeft - secondsLeft;
            progress.itemReceivedSecondsLeft = secondsLeft;
            return timeSinceReceived >= skipAfterSeconds;
        }

        // Every other time-based achievement ignores skips.
        if (foundEvent.isSkipped()) {
            return false;
        }

        if (noSkip && progress.hasSkipped) {
            return false;
        }

        // EARLY_BIRD - first player to collect.
        if (firstPlayer) {
            if (foundEvent.isBackToBack()) {
                return false; // a b2b doesn't count as "first item"
            }
            if (progress.firstItemCollected) {
                return false;
            }

            // Nobody else has a real find yet, and this player's owner has exactly the one just
            // recorded. The find lands before this runs -- FoundItemListener is registered ahead of
            // AchievementListener on the same event -- which is why the owner's own count may be 1.
            // Skips are filtered out: a rival who merely skipped an item must not block this.
            ScoreOwner own = forceItemPlayer.scoreOwner();
            boolean isFirstGlobally = world.scoreOwners().stream().allMatch(owner -> {
                long collected = owner.foundItems().stream()
                        .filter(item -> !item.usedSkip())
                        .count();
                return owner.equals(own) ? collected <= 1 : collected == 0;
            });

            if (isFirstGlobally) {
                progress.firstItemCollected = true;
                return true;
            }
            return false;
        }

        // Items collected within the last X seconds.
        // targetAmount=1 → CLOSE_CALL, 3 → BUZZER_BEATER
        if (closeCallSeconds > 0) {
            if (secondsLeft > closeCallSeconds) {
                return false;
            }
            progress.count++;
            progress.lastItemSecondsLeft = secondsLeft;
            return progress.count >= targetAmount;
        }

        // Within X seconds from game start
        if (withinSeconds > 0) {
            if (elapsedGameTime > withinSeconds) {
                return false;
            }
            progress.count++;
            progress.lastItemSecondsLeft = secondsLeft;
            return progress.count >= targetAmount;
        }

        // Took at least X seconds to find
        if (timeFrameSeconds > 0) {
            if (elapsedItemTime < timeFrameSeconds) {
                progress.count = 0;
                progress.lastItemSecondsLeft = secondsLeft;
                return false;
            }
            progress.count++;
            progress.lastItemSecondsLeft = secondsLeft;
            return progress.count >= targetAmount;
        }

        return false;
    }

    @Override
    public TimeAchievementProgress createProgress() {
        return new TimeAchievementProgress();
    }

    @Override
    public boolean isPlayerBased() {
        return playerBased;
    }
}
