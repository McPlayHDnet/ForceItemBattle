package forceitembattle.achievements.handlers;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks how many times each item type has been assigned this round.
 */
public class ItemFrequencyAchievementProgress implements AchievementProgressTracker {
    public final Map<Material, Integer> counts = new HashMap<>();
}