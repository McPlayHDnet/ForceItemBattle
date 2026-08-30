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
 * Everything an achievement rule is allowed to ask about the round it is running in.
 *
 * <h2>Why this is not the plugin</h2>
 *
 * <p>{@code check} used to take a {@link forceitembattle.ForceItemBattle}, which made the real
 * interface of every achievement rule "all 23 managers". Measured, 17 of the 22 handlers never
 * touched it and the other five between them called <em>six methods</em>. The parameter existed
 * because that was the shape every constructor already had, and it cost the whole package its test
 * surface: exercising a rule that computes nothing but arithmetic over a progress tracker meant
 * standing up a plugin.
 *
 * <p>Handlers are stateless strategies held on the {@code Achievements} enum, so they cannot be
 * given collaborators at construction — that would depend on class-load order, and the javadoc on
 * {@link forceitembattle.achievements.handlers.AchievementHandler} has always said so. Passing a
 * narrow world at call time is the shape that respects the constraint without handing out the
 * plugin.
 *
 * <p>Two adapters justify the seam: {@code PluginAchievementWorld} over the live managers, and a
 * literal in tests.
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

    /**
     * How far into the round we are.
     *
     * <p>A default rather than a field on each caller because two handlers used to subtract these
     * two numbers themselves, out of two different managers, and a comment in a third place noted
     * it was "mirroring" the same calculation elsewhere. One subtraction, one place.
     */
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
