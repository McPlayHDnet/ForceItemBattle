package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Manager;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

@Getter
@RequiredArgsConstructor
public class CommandsManager implements Manager {

    private final ForceItemBattle plugin;

    /**
     * Commands list used for /help
     */
    private final List<CustomCommand> commands = new ArrayList<>();

    public void registerCommand(CustomCommand customCommand) {
        String name = customCommand.getName();

        PluginCommand command = this.plugin.getCommand(name);
        if (command == null) {
            throw new IllegalArgumentException("Command " + name + " does not exist in plugin.yml");
        }

        command.setExecutor(customCommand);
        if (customCommand instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }

        this.commands.add(customCommand);
    }

    /**
     * Warns about commands declared in the generated plugin.yml that no {@link CustomCommand} ever
     * claimed. Call once, after every command has been registered.
     *
     * The command list is maintained twice — the {@code bukkitPluginYaml} block in build.gradle.kts
     * and {@code initCommands()} — and only one direction of drift was caught: registering an
     * executor for a name that isn't declared throws above. The other direction was silent, and the
     * two lists live in different files with nothing compiling either against the other, so a name
     * that reaches only one of them is one edit away.
     *
     * <p>The case that prompted this check was a command declared in plugin.yml under one name
     * while the {@link CustomCommand} meant to own it answered to another, leaving the declared
     * name unclaimed. Both have since gone with the player-trading feature, so the example no
     * longer exists — the failure shape does. A declared command with no executor still exists to
     * the server: it tab-completes, passes the "unknown command" check, and then does nothing but
     * print its usage line.
     */
    public void warnAboutUnboundCommands() {
        for (String name : this.plugin.getDescription().getCommands().keySet()) {
            PluginCommand command = this.plugin.getCommand(name);
            // Bukkit leaves the owning plugin as the executor when nobody sets one.
            if (command != null && command.getExecutor() == this.plugin) {
                this.plugin.getLogger().warning("Command '" + name
                        + "' is declared in plugin.yml but has no executor — it will do nothing."
                        + " Remove it from bukkitPluginYaml in build.gradle.kts, or register a"
                        + " CustomCommand for it in initCommands().");
            }
        }
    }

}
