package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandHelp extends CustomCommand {

    public CommandHelp() {
        super("help");
    }

    private void msg(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        msg(player, "&7");
        msg(player, "&6&lForceItemBattle &7- &fHelp");

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
                description = " &8- &7"+ command.getDescription();
            }

            msg(player, "&8- &f/" + usage + description);
        }
        msg(player, "&7");
    }
}
