package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command that is specified in plugin.yml
 */
@Getter
public abstract class CustomCommand implements CommandExecutor {

    protected final ForceItemBattle plugin = ForceItemBattle.getInstance();
    private final String name;
    @Setter
    private String usage;
    @Setter
    private String description;

    public CustomCommand(String name) {
        this.name = name;

        plugin.getCommandsManager().registerCommand(this);
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
