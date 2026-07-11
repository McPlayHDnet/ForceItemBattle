package forceitembattle.achievements.progress;

import org.bukkit.Material;

public class BackToBackAchievementProgress implements AchievementProgressTracker {
    public int b2bCount = 0;
    public Material previousItem = null;
    public boolean previousWasSkip = false;
}
