package forceitembattle.achievements.global;

import java.time.Instant;

public record CollectedItem(Instant firstCollected, long timesCollected) {
}
