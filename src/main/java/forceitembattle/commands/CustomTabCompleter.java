package forceitembattle.commands;

import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public interface CustomTabCompleter extends TabCompleter {

    @Override
    default List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        return onTabComplete(player, alias, args);
    }

    List<String> onTabComplete(Player player, String label, String[] args);

    /**
     * The names of everyone online — by far the most common completion, and previously a private
     * copy of this one expression in three separate commands.
     */
    static List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
