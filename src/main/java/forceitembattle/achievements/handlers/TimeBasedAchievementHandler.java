package forceitembattle.achievements.handlers;

import forceitembattle.ForceItemBattle;
import forceitembattle.achievements.Trigger;
import forceitembattle.achievements.progress.TimeAchievementProgress;
import forceitembattle.event.FoundItemEvent;
import forceitembattle.model.ForceItemPlayer;
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
    public boolean check(Event event, TimeAchievementProgress progress, ForceItemPlayer forceItemPlayer, ForceItemBattle plugin) {
        if (!(event instanceof FoundItemEvent foundEvent)) {
            return false;
        }

        ForceItemBattle fib = plugin;
        // Pause-aware game clock: the Timer only counts down during MID_GAME, so
        // deriving elapsed/remaining from it (instead of wall time) stays correct
        // across /pause and /resume. Mirrors ItemDifficultiesManager's elapsed calc.
        int gameDuration = fib.getGamemanager().getGameDuration();   // total round seconds
        int secondsLeft = fib.getTimerManager().getTimeLeft();              // seconds remaining
        long elapsedGameTime = gameDuration - secondsLeft;           // seconds since round start

        // Anchor per-item markers to the round start on first use. The first item
        // is assigned at game start, i.e. when secondsLeft == gameDuration.
        if (!progress.initialized) {
            progress.lastItemSecondsLeft = gameDuration;
            progress.itemReceivedSecondsLeft = gameDuration;
            progress.initialized = true;
        }

        long elapsedItemTime = progress.lastItemSecondsLeft - secondsLeft; // seconds since last counted item

        // Update item received marker for non-b2b, non-skip items
        if (!foundEvent.isBackToBack() && !foundEvent.isSkipped()) {
            progress.itemReceivedSecondsLeft = secondsLeft;
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
            long timeSinceReceived = progress.itemReceivedSecondsLeft - secondsLeft;
            progress.itemReceivedSecondsLeft = secondsLeft;
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
            if (progress.firstItemCollected) {
                return false;
            }

            boolean isFirstGlobally;
            if (forceItemPlayer.currentTeam() != null) {
                var ownTeam = forceItemPlayer.currentTeam();
                isFirstGlobally = fib.getTeamManager().getTeams().stream().allMatch(team -> {
                    long collected = team.getFoundItems().stream()
                            .filter(item -> !item.usedSkip())
                            .count();
                    return team.equals(ownTeam) ? collected <= 1 : collected == 0;
                });
            } else {
                isFirstGlobally = fib.getGamemanager().forceItemPlayerMap().values().stream()
                        .filter(p -> !p.isSpectator())
                        .allMatch(p -> p.foundItems().isEmpty() ||
                                p.player().getUniqueId().equals(forceItemPlayer.player().getUniqueId()));
            }

            if (isFirstGlobally) {
                progress.firstItemCollected = true;
                return true;
            }
            return false;
        }

        // CLOSE CALL / final-window - count items collected within the last X seconds.
        // targetAmount=1 → CLOSE_CALL (one item in the window)
        // targetAmount=3 → BUZZER_BEATER (three items in the window)
        if (closeCallSeconds > 0) {
            if (secondsLeft > closeCallSeconds) {
                return false; // Not in the final window yet
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
