package forceitembattle.gui;

import forceitembattle.achievements.AchievementManager;
import forceitembattle.collection.CollectionManager;
import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.service.FIBServiceClient;
import org.bukkit.plugin.Plugin;

/**
 * What the achievement and collection menus are allowed to reach.
 *
 * <p>The same shape, and the same reason, as {@link forceitembattle.commands.CommandContext}: taking
 * the plugin would make a menu's real interface every manager on it. Five named things instead.
 *
 * <p>It exists for these four menus specifically because they navigate <em>into each other</em> —
 * the category grid opens a scope page, which opens the collection book, which opens a category dex,
 * and every one of them has a back button to the one above. Each therefore has to be able to
 * construct the others, so each needs the union of what all four use, and passing five arguments
 * through four constructors four times over is how they drift apart. Every other menu in this
 * package takes its dependencies directly, because no other menu opens one of these.
 *
 * @param plugin only ever for {@code getLogger()} — no menu schedules anything through it
 */
public record GuiContext(Plugin plugin,
                         AchievementManager achievements,
                         CollectionManager collection,
                         ItemDifficultiesManager items,
                         FIBServiceClient service) {
}
