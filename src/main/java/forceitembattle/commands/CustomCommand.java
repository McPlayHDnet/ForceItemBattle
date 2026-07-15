package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Text;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command that is specified in plugin.yml.
 *
 * <p>The owning plugin is injected via the constructor. Registration is no longer
 * a constructor side effect — commands are registered explicitly through
 * {@link CommandsManager#registerCommand(CustomCommand)} during bootstrap.
 */
@Getter
public abstract class CustomCommand implements CommandExecutor {

    protected final ForceItemBattle plugin;
    private final String name;
    @Setter
    private String usage;
    @Setter
    private String description;

    public CustomCommand(ForceItemBattle plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public void msgUsage(Player player) {
        String usage = this.getUsage() == null ? "" : " " + this.getUsage();
        String description = this.getDescription() == null ? "uhhh I guess this is self explanatory?.." : this.getDescription();

        player.sendMessage(Text.of("<dark_gray>» <white>/" + this.getName() + "<gray>" + usage + " <dark_gray>- <white>" + description));
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

    protected static final String NO_PERMISSION = "<red>You don't have permission to use this command.";

    protected boolean requireOp(Player player) {
        if (player.isOp()) {
            return true;
        }
        player.sendMessage(Text.of(NO_PERMISSION));
        return false;
    }

}
