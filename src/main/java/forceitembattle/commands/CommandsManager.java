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

}