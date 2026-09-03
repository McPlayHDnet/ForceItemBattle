package forceitembattle.achievements;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Where an unlock goes, with the transport taken out.
 *
 * <p>{@link AchievementStorage} owns the cache and the de-dup; this owns the wire. Splitting them is
 * what makes everything above the storage — the end-of-round rules, the Completionist tiers, the
 * whole grant path — reachable from a test, because until now the manager built its own storage,
 * which built its own HTTP client, and nothing in the chain could be substituted.
 *
 * <p>The production implementation is {@link ServiceAchievementSink}; tests use a recording one.
 */
public interface AchievementSink {

    /**
     * Every achievement id this player already holds.
     *
     * <p>Exactly one of the two callbacks runs. {@code onFailure} exists rather than an empty set
     * because the two mean different things to the cache: an empty set is a player with no unlocks
     * and is worth remembering, a failure is not, and remembering it would grant every achievement
     * a second time the moment the service came back.
     */
    void load(UUID playerUuid, Consumer<Set<String>> onLoaded, Runnable onFailure);

    void unlock(UUID playerUuid, Achievements achievement, AchievementMode mode,
                @Nullable UUID teammateUuid);

    void remove(UUID playerUuid, Achievements achievement);

    void reset(UUID playerUuid);
}
