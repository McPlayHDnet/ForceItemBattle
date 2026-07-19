package forceitembattle.achievements;

import forceitembattle.achievements.global.GlobalRule;
import lombok.Getter;

@Getter
public enum AchievementScope {

    /** Evaluated inside a single round, from the in-game event stream, via an {@code AchievementHandler}. */
    ROUND("Round Achievements", "Earned within a single round"),

    /** Evaluated across every round ever played, from persisted stats, via a {@link GlobalRule}. */
    GLOBAL("Global Achievements", "Earned across every round you play"),

    /** Evaluated against the player's other unlocks, via a {@link CompletionistRule}. */
    META("Meta Achievements", "Earned by completing other achievements"),

    /** Evaluated across every round, from match history, via a {@link CollectionRule}. */
    COLLECTION("Collection Achievements", "Earned by collecting every item");

    private final String displayName;
    private final String subtitle;

    AchievementScope(String displayName, String subtitle) {
        this.displayName = displayName;
        this.subtitle = subtitle;
    }
}
