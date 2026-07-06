package forceitembattle.achievements.handlers;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

/**
 * Tracks how many times each item type has been assigned this round.
 */
public class ItemFrequencyAchievementProgress implements AchievementProgressTracker {
    public final Map<Material, Integer> counts = new HashMap<>();
}
