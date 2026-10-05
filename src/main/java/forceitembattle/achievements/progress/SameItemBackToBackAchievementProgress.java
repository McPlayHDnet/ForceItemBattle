package forceitembattle.achievements.progress;

import lombok.ToString;
import org.bukkit.Material;

@ToString
public class SameItemBackToBackAchievementProgress {

    public Material lastBackToBackItem = null;

    public int sameItemStreak = 0;

}
