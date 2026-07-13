package forceitembattle.achievements;

import forceitembattle.ForceItemBattle;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record CompletionistRule(Set<AchievementScope> requiredScopes) {

    public CompletionistRule(AchievementScope first, AchievementScope... rest) {
        this(EnumSet.of(first, rest));
    }

    public CompletionistRule {
        if (requiredScopes.contains(AchievementScope.META)) {
            throw new IllegalArgumentException("A Completionist tier cannot require META achievements");
        }
    }

    public boolean isMet(ForceItemBattle plugin, UUID playerUuid) {
        AchievementStorage storage = plugin.getAchievementManager().getAchievementStorage();

        for (Achievements achievement : Achievements.values()) {
            if (!requiredScopes.contains(achievement.getScope())) {
                continue;
            }
            if (!storage.hasAchievement(playerUuid, achievement)) {
                return false;
            }
        }
        return true;
    }
}
