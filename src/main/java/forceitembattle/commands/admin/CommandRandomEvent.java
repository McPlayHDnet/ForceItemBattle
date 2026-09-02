package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.randomevents.RandomEventManager;
import forceitembattle.randomevents.RandomEvents;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandRandomEvent extends CustomCommand implements CustomTabCompleter {

    private final RandomEventManager randomEventManager;

    public CommandRandomEvent(RandomEventManager randomEventManager) {
        super("randomevent");
        this.randomEventManager = randomEventManager;
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

        if (!this.randomEventManager.trigger(type)) {
            player.sendMessage(Text.of("<red>An event is already running."));
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return RandomEvents.ids();
    }
}
