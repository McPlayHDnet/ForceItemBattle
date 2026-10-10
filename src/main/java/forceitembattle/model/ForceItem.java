package forceitembattle.model;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Material;

/** @param collectedBy who handed the item in, which matters in team mode; nullable for older records */
public record ForceItem(Material material, String timeNeeded, long timeStamp, BackToBack back2Back,
                        boolean usedSkip, @Nullable UUID collectedBy) {
}
