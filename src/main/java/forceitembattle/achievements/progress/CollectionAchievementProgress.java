package forceitembattle.achievements.progress;

import java.util.HashSet;
import java.util.Set;

public class CollectionAchievementProgress<T> implements AchievementProgressTracker {
    public final Set<T> collected = new HashSet<>();
    public LastCheckedPosition lastPosition = null;

    /**
     * Tracks the last block position checked to avoid redundant biome lookups
     */
    public record LastCheckedPosition(int x, int y, int z) {
    }
}
