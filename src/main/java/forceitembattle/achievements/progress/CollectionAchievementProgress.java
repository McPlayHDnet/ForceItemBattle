package forceitembattle.achievements.progress;

import java.util.HashSet;
import java.util.Set;
import lombok.ToString;

@ToString
public class CollectionAchievementProgress<T> {
    public final Set<T> collected = new HashSet<>();
    public LastCheckedPosition lastPosition = null;

    public record LastCheckedPosition(int x, int y, int z) {
    }
}
