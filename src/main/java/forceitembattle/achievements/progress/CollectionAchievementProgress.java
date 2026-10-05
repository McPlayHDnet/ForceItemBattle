package forceitembattle.achievements.progress;

import java.util.HashSet;
import java.util.Set;

public class CollectionAchievementProgress<T> {
    public final Set<T> collected = new HashSet<>();
    public LastCheckedPosition lastPosition = null;

    public record LastCheckedPosition(int x, int y, int z) {
    }
}
