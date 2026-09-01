package forceitembattle.achievements;

import forceitembattle.model.Dimension;
import forceitembattle.model.ScoreOwner;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Everything an achievement rule is allowed to ask about the round it is running in — deliberately
 * this rather than the plugin. Widen it by one named question when a rule needs something new. Two
 * adapters: {@code PluginAchievementWorld} over the live managers, and a literal in tests.
 */
public interface AchievementWorld {

    /** The round's total length in seconds, as {@code /start} set it. */
    int roundDuration();

    /**
     * Seconds still to play. From the round clock, which only advances during MID_GAME, so anything
     * derived from it stays correct across {@code /pause} where wall time would not.
     */
    int secondsLeft();

    default int elapsedSeconds() {
        return this.roundDuration() - this.secondsLeft();
    }

    /** The materials the pool can hand out in a given dimension. */
    Set<Material> itemsIn(Dimension dimension);

    boolean isTrading(UUID playerId);

    boolean backpackEnabled();

    @Nullable
    Inventory backpackOf(Player player);

    /**
     * Everyone still scoring, one entry per owner: one per solo player, one per team however many
     * members it has. Spectators are not included.
     */
    List<ScoreOwner> scoreOwners();
}
