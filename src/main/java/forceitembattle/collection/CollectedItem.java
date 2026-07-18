package forceitembattle.collection;

import java.time.Instant;

public record CollectedItem(Instant firstCollected, long timesCollected) {
}
