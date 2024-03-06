package forceitembattle.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public interface TabCompletion extends TabCompleter {

    @Override
    default List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        return onTabComplete(player, alias, args);
    }

    List<String> onTabComplete(Player player, String label, String[] args);
}
