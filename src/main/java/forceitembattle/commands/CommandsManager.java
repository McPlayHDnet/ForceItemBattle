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

    /** Used for /help. */
    private final List<CustomCommand> commands = new ArrayList<>();

    public void registerCommand(CustomCommand customCommand) {
        String name = customCommand.getName();

        PluginCommand command = this.plugin.getCommand(name);
        if (command == null) {
            throw new IllegalArgumentException("Command " + name + " does not exist in plugin.yml");
        }

        customCommand.setContext(new CommandContext(
                this.plugin.getRoundPhase(),
                this.plugin.getSettings().getRuleset(),
                this.plugin.getRoster()));

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
     * <p>The list is maintained twice — the {@code bukkitPluginYaml} block in build.gradle.kts and
     * {@code initCommands()} — and only one direction of drift throws: registering an executor for an
     * undeclared name. The other is silent, and a declared command with no executor still exists to
     * the server, tab-completing and passing the "unknown command" check before doing nothing.
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
