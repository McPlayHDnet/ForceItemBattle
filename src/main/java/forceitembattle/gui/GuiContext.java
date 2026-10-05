package forceitembattle.gui;

import forceitembattle.achievements.AchievementManager;
import forceitembattle.collection.CollectionManager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.service.FIBServiceClient;
import org.bukkit.plugin.Plugin;

/**
 * Shared by the four menus that navigate into each other, so each can construct the others.
 *
 * @param plugin only for {@code getLogger()}
 */
public record GuiContext(Plugin plugin,
                         AchievementManager achievements,
                         CollectionManager collection,
                         ItemDifficultiesManager items,
                         FIBServiceClient service) {
}
