package forceitembattle.achievements;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Where an unlock goes, with the transport taken out, so {@link AchievementStorage} and everything
 * built on it runs without a service behind it.
 */
public interface AchievementSink {

    /**
     * Every achievement id this player holds; exactly one callback runs.
     *
     * <p>{@code onFailure} rather than an empty set, because the two mean different things to the
     * cache: no unlocks is worth remembering, a failed load is not — remembering it would re-grant
     * every achievement once the service came back.
     */
    void load(UUID playerUuid, Consumer<Set<String>> onLoaded, Runnable onFailure);

    void unlock(UUID playerUuid, Achievements achievement, AchievementMode mode,
                @Nullable UUID teammateUuid);

    void remove(UUID playerUuid, Achievements achievement);

    void reset(UUID playerUuid);
}
