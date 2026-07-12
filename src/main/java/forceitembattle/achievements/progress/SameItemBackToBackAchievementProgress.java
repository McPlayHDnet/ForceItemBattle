package forceitembattle.achievements.progress;

import org.bukkit.Material;

public class SameItemBackToBackAchievementProgress implements AchievementProgressTracker {

    public Material lastBackToBackItem = null;

    public int sameItemStreak = 0;

}
