package forceitembattle.model.stats;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

/** Can be unlocked more than once (solo and per teammate), so the GUI holds a list per id. */
public record AchievementUnlock(String achievementId, @Nullable String mode,
                                @Nullable PlayerIdentity teammate,
                                @Nullable OffsetDateTime unlockedAt) {

    /** Whether this unlock happened in a team game. The service spells the mode; nothing else does. */
    public boolean inTeam() {
        return "TEAM".equalsIgnoreCase(this.mode);
    }
}
