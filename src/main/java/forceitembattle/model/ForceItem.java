package forceitembattle.model;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Material;

/**
 * @param collectedBy who handed the item in. Only interesting in team mode, where the found list
 *                    belongs to the team rather than to one player, so the individual contribution
 *                    would otherwise be lost. Nullable for safety on items recorded without it.
 */
public record ForceItem(Material material, String timeNeeded, long timeStamp, BackToBack back2Back,
                        boolean usedSkip, @Nullable UUID collectedBy) {
}
