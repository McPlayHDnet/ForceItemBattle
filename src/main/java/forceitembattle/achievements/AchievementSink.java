package forceitembattle.achievements;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public interface AchievementSink {

    /**
     * Exactly one callback runs. A failed load must not look like "no unlocks": caching that would
     * re-grant every achievement once the service came back.
     */
    void load(UUID playerUuid, Consumer<Set<String>> onLoaded, Runnable onFailure);

    void unlock(UUID playerUuid, Achievements achievement, AchievementMode mode,
                @Nullable UUID teammateUuid);

    void remove(UUID playerUuid, Achievements achievement);

    void reset(UUID playerUuid);
}
