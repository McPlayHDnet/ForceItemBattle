package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.randomevents.RandomEvents;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.entity.Player;

public class CommandRandomEvent extends CustomCommand implements CustomTabCompleter {

    public CommandRandomEvent(ForceItemBattle plugin) {
        super(plugin, "randomevent");
        setUsage("<event>");
        setDescription("Trigger a random event right now");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) return;

        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(Text.of("<red>The game is not running. Start it first with /start"));
            return;
        }

        if (args.length != 1) {
            msgUsage(player);
            player.sendMessage(Text.of("<gray>Events <dark_gray>» <yellow>" + String.join("<gray>, <yellow>", RandomEvents.ids())));
            return;
        }

        RandomEvents type = RandomEvents.byId(args[0]);
        if (type == null) {
            player.sendMessage(Text.of("<yellow>" + args[0] + " <red>is not an event."));
            return;
        }

        if (!this.plugin.getRandomEventManager().trigger(type)) {
            player.sendMessage(Text.of("<red>An event is already running."));
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return RandomEvents.ids();
    }
}
