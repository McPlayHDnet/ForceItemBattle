package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.Precondition;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.randomevents.RandomEvents;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandRandomEvent extends CustomCommand implements CustomTabCompleter {

    public CommandRandomEvent(ForceItemBattle plugin) {
        super(plugin, "randomevent");
        setUsage("<event>");
        setDescription("Trigger a random event right now");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP, ROUND_RUNNING);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
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
