package forceitembattle.achievements.handlers;

import org.bukkit.Material;

public class AchievementBackToBackAchievementProgress implements AchievementProgressTracker {
    public int b2bCount = 0;
    public Material lastItemType = null;
    public Material lastSkippedItem = null;
}