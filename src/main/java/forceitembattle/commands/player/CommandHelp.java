package forceitembattle.commands.player;

import forceitembattle.commands.CommandsManager;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandHelp extends CustomCommand {

    private final CommandsManager commandsManager;

    public CommandHelp(CommandsManager commandsManager) {
        super("help");
        this.commandsManager = commandsManager;
    }

    private void msg(Player player, String message) {
        player.sendMessage(Text.of(message));
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        msg(player, "<gray>");
        msg(player, "<gold><b>ForceItemBattle</b> <gray>- <white>Help");

        for (CustomCommand command : this.commandsManager.getCommands()) {
            if (command instanceof CommandHelp) {
                continue;
            }

            String usage = command.getName();
            if (command.getUsage() != null) {
                usage += " " + command.getUsage();
            }

            String description = "";
            if (command.getDescription() != null) {
                description = " <dark_gray>- <gray>" + command.getDescription();
            }

            msg(player, "<dark_gray>- <white>/" + usage + description);
        }
        msg(player, "<gray>");
    }
}
