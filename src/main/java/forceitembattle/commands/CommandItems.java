package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ItemsInventory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandItems implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if(player.isOp()) new ItemsInventory(player).open(player);
        else player.sendMessage("§cNo perms lol");

        return false;
    }
}
