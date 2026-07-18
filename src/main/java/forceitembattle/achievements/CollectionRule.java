package forceitembattle.achievements;

import java.util.Set;

public record CollectionRule(double requiredFraction) {

    public CollectionRule {
        if (requiredFraction <= 0.0 || requiredFraction > 1.0) {
            throw new IllegalArgumentException("requiredFraction must be in (0, 1], got " + requiredFraction);
        }
    }

    /**
     * Items needed against the current catalogue. Rounded up, so a tier always demands strictly
     * more than the fraction rather than less, and 100% means every single item.
     */
    public int requiredCount(int catalogueSize) {
        return (int) Math.ceil(catalogueSize * this.requiredFraction);
    }

    /** How many catalogue items the player has -- extras outside the catalogue don't count. */
    public int collectedCount(Set<String> foundItemKeys, Set<String> catalogueKeys) {
        int collected = 0;
        for (String key : catalogueKeys) {
            if (foundItemKeys.contains(key)) {
                collected++;
            }
        }
        return collected;
    }

    public boolean isMet(Set<String> foundItemKeys, Set<String> catalogueKeys) {
        // Empty catalogue guard: if the item registry hasn't populated, never grant.
        if (catalogueKeys.isEmpty()) {
            return false;
        }
        return collectedCount(foundItemKeys, catalogueKeys) >= requiredCount(catalogueKeys.size());
    }
}
