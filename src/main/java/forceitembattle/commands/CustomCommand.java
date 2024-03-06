package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Command that is specified in plugin.yml
 */
public abstract class CustomCommand implements CommandExecutor {

    // TODO rename to plugin or sth
    protected final ForceItemBattle forceItemBattle = ForceItemBattle.getInstance();
    private final String name;

    public CustomCommand(String name) {
        this.name = name;

        PluginCommand command = this.forceItemBattle.getCommand(name);
        if (command == null) {
            throw new IllegalArgumentException("Command " + name + " does not exist in plugin.yml");
        }

        command.setExecutor(this);
        if (this instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            this.onPlayerCommand(player, label, args);
        } else {
            this.onConsoleCommand(sender, label, args);
        }
        return true;
    }

    public abstract void onPlayerCommand(Player player, String label, String[] args);

    // Override this for console commands.
    public void onConsoleCommand(CommandSender sender, String label, String[] args) {
        sender.sendMessage("This command can only be executed by a player");
    }

}
