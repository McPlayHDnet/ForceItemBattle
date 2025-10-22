package forceitembattle.settings.achievements.handlers;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CollectionProgress<T> implements ProgressTracker {
    public final Set<T> collected = new HashSet<>();
    public LastCheckedPosition lastPosition = null;

    /**
     * Tracks the last block position checked to avoid redundant biome lookups
     */
    public static class LastCheckedPosition {
        public final int x, y, z;

        public LastCheckedPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LastCheckedPosition other)) return false;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}