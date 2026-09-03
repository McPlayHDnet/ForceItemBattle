package forceitembattle.achievements;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * The second implementation of {@link AchievementSink}, and the reason the interface is worth having.
 *
 * <p>{@link AchievementStorage} used to build its own HTTP client, so nothing above it could run
 * without a service. Every unlock lands here instead, with the player and the mode it was recorded
 * under, because "which mode" is part of what the rules decide.
 */
final class RecordingAchievementSink implements AchievementSink {

    record Unlock(UUID player, Achievements achievement, AchievementMode mode, UUID teammate) {
    }

    final List<Unlock> unlocks = new ArrayList<>();
    final List<UUID> removals = new ArrayList<>();
    final List<UUID> resets = new ArrayList<>();

    /** What a load returns, per player. Absent means the load fails. */
    private final java.util.Map<UUID, Set<String>> held = new java.util.HashMap<>();
    private boolean failLoads;

    void holds(UUID player, String... achievementIds) {
        this.held.put(player, new LinkedHashSet<>(List.of(achievementIds)));
    }

    void failLoads() {
        this.failLoads = true;
    }

    @Override
    public void load(UUID playerUuid, Consumer<Set<String>> onLoaded, Runnable onFailure) {
        if (this.failLoads) {
            onFailure.run();
            return;
        }
        onLoaded.accept(this.held.getOrDefault(playerUuid, Set.of()));
    }

    @Override
    public void unlock(UUID playerUuid, Achievements achievement, AchievementMode mode,
                       @Nullable UUID teammateUuid) {
        this.unlocks.add(new Unlock(playerUuid, achievement, mode, teammateUuid));
    }

    @Override
    public void remove(UUID playerUuid, Achievements achievement) {
        this.removals.add(playerUuid);
    }

    @Override
    public void reset(UUID playerUuid) {
        this.resets.add(playerUuid);
    }

    /** Which achievements this player was granted, in order. */
    List<Achievements> granted(UUID player) {
        return this.unlocks.stream()
                .filter(unlock -> unlock.player().equals(player))
                .map(Unlock::achievement)
                .toList();
    }
}
