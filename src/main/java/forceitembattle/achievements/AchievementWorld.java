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
 * Everything an achievement rule is allowed to ask about the round it is running in. See
 * {@code CONTEXT.md § Achievement World} for why it is this and not the plugin.
 *
 * <p>Widen it by one named question when a rule needs something new. Two adapters:
 * {@code PluginAchievementWorld} over the live managers, and a literal in tests.
 */
public interface AchievementWorld {

    /** The round's total length in seconds, as {@code /start} set it. */
    int roundDuration();

    /**
     * Seconds still to play.
     *
     * <p>Read from the round clock, which only advances during MID_GAME — so anything derived from
     * it stays correct across {@code /pause} and {@code /resume}, where wall time would not.
     */
    int secondsLeft();

    /** How far into the round we are. One subtraction, one place — see the class note. */
    default int elapsedSeconds() {
        return this.roundDuration() - this.secondsLeft();
    }

    /** The materials the pool can hand out in a given dimension. */
    Set<Material> itemsIn(Dimension dimension);

    /** Whether this player is mid-trade with one of the round's wandering traders. */
    boolean isTrading(UUID playerId);

    /** Whether the backpack is switched on for this round. */
    boolean backpackEnabled();

    /** This player's backpack, or {@code null} if they have none. */
    @Nullable
    Inventory backpackOf(Player player);

    /**
     * Everyone still scoring in this round, one entry per owner: one per solo player, one per team
     * however many members it has. Spectators are not included.
     */
    List<ScoreOwner> scoreOwners();
}
