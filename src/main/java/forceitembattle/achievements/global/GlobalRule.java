package forceitembattle.achievements.global;

import forceitembattle.achievements.AchievementScope;

/**
 * Unlock condition for a {@link AchievementScope#GLOBAL} achievement: the player's
 * cumulative {@code stat} reaching {@code threshold}.
 */
public record GlobalRule(GlobalStat stat, long threshold) {

    public GlobalRule {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Global threshold must be positive, got " + threshold);
        }
    }

    public boolean isMet(long current) {
        return current >= threshold;
    }
}
