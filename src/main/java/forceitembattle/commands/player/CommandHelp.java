package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandHelp extends CustomCommand {

    public CommandHelp() {
        super("help");
    }

    private void msg(Player player, String message) {
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(message));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        msg(player, "<gray>");
        msg(player, "<gold><b>ForceItemBattle</b> <gray>- <white>Help");

        for (CustomCommand command : plugin.getCommandsManager().getCommands()) {
            if (command instanceof CommandHelp) {
                continue;
            }

            String usage = command.getName();
            if (command.getUsage() != null) {
                usage += " " + command.getUsage();
            }

            String description = "";
            if (command.getDescription() != null) {
                description = " <dark_gray>- <gray>"+ command.getDescription();
            }

            msg(player, "<dark_gray>- <white>/" + usage + description);
        }
        msg(player, "<gray>");
    }
}
