package forceitembattle.randomevents;

import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.model.RoundPhase;
import forceitembattle.settings.GameSettings;
import org.bukkit.plugin.Plugin;

/**
 * What a {@link RandomEvent} is allowed to reach.
 *
 * <p>The same device as {@link forceitembattle.commands.CommandContext} and
 * {@link forceitembattle.gui.GuiContext}. Events are built reflectively through
 * {@link RandomEvents#create}, so they all share one factory signature — which means the signature
 * is the union of what any event needs, and taking the plugin made that union "everything".
 *
 * @param plugin only for {@code getLogger()}
 */
public record EventContext(Plugin plugin,
                           RoundPhase roundPhase,
                           GameSettings settings,
                           ItemDifficultiesManager items,
                           WanderingTraderManager traders) {
}
