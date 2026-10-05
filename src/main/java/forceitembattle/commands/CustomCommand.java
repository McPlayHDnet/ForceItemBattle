package forceitembattle.commands;

import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.util.List;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Registered via {@link CommandsManager#registerCommand}; runs only once its {@link #preconditions()} hold. */
@Getter
public abstract class CustomCommand implements CommandExecutor {

    private final String name;
    private String usage;
    private String description;

    /** Set at registration rather than in the constructor, to avoid threading it through every subclass. */
    private CommandContext context;

    public CustomCommand(String name) {
        this.name = name;
    }

    /** Checked in order; the first failure is reported. Abstract so a forgotten gate can't look like "none". */
    protected abstract List<Precondition> preconditions();

    /** Reads the declaration from outside the subclass, for the pinned table in the tests. */
    final List<Precondition> declaredPreconditions() {
        return this.preconditions();
    }

    /** Whether a non-op would be refused right now. Subcommand gates via {@code requireOp} are not seen. */
    public final boolean isOpOnly() {
        return this.preconditions().stream().anyMatch(precondition ->
                precondition.label().equals(Precondition.OP.label())
                        || precondition.label().equals(Precondition.OP_WHEN_EVENT.label())
                        && this.context != null
                        && this.context.settingEnabled(GameSetting.EVENT));
    }

    final void setContext(CommandContext context) {
        this.context = context;
    }

    public final void setUsage(String usage) {
        this.usage = usage;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    public void msgUsage(Player player) {
        String usage = this.getUsage() == null ? "" : " " + this.getUsage();
        String description = this.getDescription() == null ? "uhhh I guess this is self explanatory?.." : this.getDescription();

        player.sendMessage(Text.of("<dark_gray>» <white>/" + this.getName() + "<gray>" + usage + " <dark_gray>- <white>" + description));
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        for (Precondition precondition : this.preconditions()) {
            if (!precondition.holds(sender, this.context)) {
                sender.sendMessage(Text.of(precondition.refusal()));
                return true;
            }
        }

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

    /** For subcommand gates only, where a command-level {@link Precondition#OP} cannot reach {@code args[0]}. */
    protected final void requireOp(Player player, Runnable action) {
        if (!player.isOp()) {
            player.sendMessage(Text.of(Precondition.NO_PERMISSION));
            return;
        }
        action.run();
    }
}
