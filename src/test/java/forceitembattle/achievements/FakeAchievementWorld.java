package forceitembattle.achievements;

import forceitembattle.model.Dimension;
import forceitembattle.model.ScoreOwner;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * The second adapter, and the reason the seam is real rather than hypothetical.
 *
 * <p>Everything an achievement rule may ask, answered from fields a test sets by hand. Before
 * {@link AchievementWorld} existed there was no way to write this: {@code check} took a
 * {@code ForceItemBattle}, so standing up one rule meant standing up 23 managers, and the whole
 * package went untested as a result.
 */
final class FakeAchievementWorld implements AchievementWorld {

    private int roundDuration = 3600;
    private int secondsLeft = 3600;
    private final Map<Dimension, Set<Material>> itemsByDimension = new EnumMap<>(Dimension.class);
    private final Set<UUID> trading = new HashSet<>();
    private boolean backpackEnabled;
    @Nullable
    private Inventory backpack;
    private final List<ScoreOwner> scoreOwners = new ArrayList<>();

    /** Places the clock: a round of {@code duration} seconds with {@code left} still to play. */
    FakeAchievementWorld clock(int duration, int left) {
        this.roundDuration = duration;
        this.secondsLeft = left;
        return this;
    }

    /** Moves the clock forward without changing the round's length. */
    FakeAchievementWorld secondsLeft(int left) {
        this.secondsLeft = left;
        return this;
    }

    FakeAchievementWorld itemsIn(Dimension dimension, Material... materials) {
        this.itemsByDimension.put(dimension, Set.of(materials));
        return this;
    }

    FakeAchievementWorld trading(UUID playerId) {
        this.trading.add(playerId);
        return this;
    }

    FakeAchievementWorld backpack(boolean enabled, @Nullable Inventory inventory) {
        this.backpackEnabled = enabled;
        this.backpack = inventory;
        return this;
    }

    FakeAchievementWorld scoreOwners(ScoreOwner... owners) {
        this.scoreOwners.clear();
        this.scoreOwners.addAll(List.of(owners));
        return this;
    }

    @Override
    public int roundDuration() {
        return this.roundDuration;
    }

    @Override
    public int secondsLeft() {
        return this.secondsLeft;
    }

    @Override
    public Set<Material> itemsIn(Dimension dimension) {
        return this.itemsByDimension.getOrDefault(dimension, Set.of());
    }

    @Override
    public boolean isTrading(UUID playerId) {
        return this.trading.contains(playerId);
    }

    @Override
    public boolean backpackEnabled() {
        return this.backpackEnabled;
    }

    @Override
    @Nullable
    public Inventory backpackOf(Player player) {
        return this.backpack;
    }

    @Override
    public List<ScoreOwner> scoreOwners() {
        return List.copyOf(this.scoreOwners);
    }
}
