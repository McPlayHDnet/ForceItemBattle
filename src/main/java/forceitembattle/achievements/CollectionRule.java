package forceitembattle.achievements;

import java.util.Set;

/**
 * Unlock condition for a {@link AchievementScope#COLLECTION} achievement: the player's set of
 * found item keys covers the entire catalogue. Both sets are supplied by the evaluator
 * (AchievementManager) -- the catalogue is session-dynamic (drawn from ItemDifficultiesManager),
 * so it can't live on the enum; the rule itself is pure set containment.
 *
 * A carrier object like GlobalRule / CompletionistRule, so a COLLECTION achievement declares
 * itself the same way the others do: {@code new CollectionRule()}.
 */
public record CollectionRule() {

    public boolean isMet(Set<String> foundItemKeys, Set<String> catalogueKeys) {
        // Empty catalogue guard: if the item registry hasn't populated, never grant.
        return !catalogueKeys.isEmpty() && foundItemKeys.containsAll(catalogueKeys);
    }
}
