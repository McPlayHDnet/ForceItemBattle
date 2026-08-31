package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Text;
import java.util.List;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command that is specified in plugin.yml.
 *
 * <p>Constructing one does not register it: that happens explicitly through
 * {@link CommandsManager#registerCommand(CustomCommand)} during bootstrap.
 *
 * <p>A command <b>declares</b> what it requires in {@link #preconditions()} and is invoked only
 * once those hold — it does not check for itself. The rules the declaration obeys, and why
 * subcommand-level gates are not part of it, are on {@link Precondition}.
 */
@Getter
public abstract class CustomCommand implements CommandExecutor {

    protected final ForceItemBattle plugin;
    private final String name;
    private String usage;
    private String description;

    /**
     * Set by {@link CommandsManager#registerCommand} at bootstrap, and by tests directly.
     *
     * <p>Not a constructor parameter: that would put it through all 34 subclass constructors to
     * serve a check none of them performs themselves.
     */
    private CommandContext context;

    public CustomCommand(ForceItemBattle plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    /**
     * What must hold before this command's body runs, in the order it is checked — the first
     * failure is what the sender is told. Return {@link List#of()} for a command with no gates.
     *
     * <p>Abstract on purpose. A default would make declaring optional, and "forgot to declare a
     * gate" would once again look exactly like "correctly has none" — which is the shape that let
     * {@code /skip} run its whole body for non-ops only.
     */
    protected abstract List<Precondition> preconditions();

    /** Reads the declaration from outside the subclass, for the pinned table in the tests. */
    final List<Precondition> declaredPreconditions() {
        return this.preconditions();
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

    /**
     * Runs {@code action} only if the player is an op, and refuses them otherwise.
     *
     * <p><b>For subcommand gates only</b> — {@code CommandAchievement} and {@code CommandStats},
     * where the gate hangs off {@code args[0]} and a command-level declaration cannot reach it.
     * Everything else declares {@link Precondition#OP}.
     *
     * <p>Takes the body rather than returning a boolean, which is the form {@code
     * CommandAchievement} had already invented for itself. A boolean exists to be put in an
     * {@code if}, and putting it in an {@code if} is what inverted {@code /skip}.
     */
    protected final void requireOp(Player player, Runnable action) {
        if (!player.isOp()) {
            player.sendMessage(Text.of(Precondition.NO_PERMISSION));
            return;
        }
        action.run();
    }
}
