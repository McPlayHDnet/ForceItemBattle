package forceitembattle.settings.achievements;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface AchievementRequirement {
    boolean isMet(Player player, Event event);
    void reset(Player player);
}
