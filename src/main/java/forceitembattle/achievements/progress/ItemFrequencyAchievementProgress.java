package forceitembattle.achievements.progress;

import java.util.HashMap;
import java.util.Map;
import lombok.ToString;
import org.bukkit.Material;

@ToString
public class ItemFrequencyAchievementProgress {
    public final Map<Material, Integer> counts = new HashMap<>();
}
