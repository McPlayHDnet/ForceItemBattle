package forceitembattle.model;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

/**
 * One record of a player unlocking an achievement: which one, whether they were solo or in a team,
 * with whom, and when.
 *
 * <p>An achievement can be unlocked more than once — once solo and once per teammate — which is why
 * the GUI holds a list per achievement id rather than a flag.
 */
public record AchievementUnlock(String achievementId, @Nullable String mode,
                                @Nullable PlayerIdentity teammate,
                                @Nullable OffsetDateTime unlockedAt) {

    /** Whether this unlock happened in a team game. The service spells the mode; nothing else does. */
    public boolean inTeam() {
        return "TEAM".equalsIgnoreCase(this.mode);
    }
}
