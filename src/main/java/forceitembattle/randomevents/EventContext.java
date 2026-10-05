package forceitembattle.randomevents;

import forceitembattle.manager.ItemDifficultiesManager;
import forceitembattle.manager.WanderingTraderManager;
import forceitembattle.model.RoundPhase;
import forceitembattle.settings.GameSettings;
import org.bukkit.plugin.Plugin;

/**
 * Events are built reflectively through {@link RandomEvents#create}, so this is the union of what any event needs.
 *
 * @param plugin only for {@code getLogger()}
 */
public record EventContext(Plugin plugin,
                           RoundPhase roundPhase,
                           GameSettings settings,
                           ItemDifficultiesManager items,
                           WanderingTraderManager traders) {
}
