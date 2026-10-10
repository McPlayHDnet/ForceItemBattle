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

public interface AchievementWorld {

    /** The round's total length in seconds, as {@code /start} set it. */
    int roundDuration();

    /** From the round clock, which only advances during MID_GAME, so it stays correct across /pause. */
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

    /** One entry per owner (a team counts once); spectators excluded. */
    List<ScoreOwner> scoreOwners();
}
