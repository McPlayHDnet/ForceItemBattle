package forceitembattle.achievements.progress;

import lombok.ToString;
import org.bukkit.Material;

@ToString
public class BackToBackAchievementProgress {
    public int b2bCount = 0;
    public Material previousItem = null;
    public boolean previousWasSkip = false;
}
