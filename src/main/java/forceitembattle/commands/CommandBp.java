package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBp implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack")) return false;
        if (!ForceItemBattle.getTimer().isRunning()) return false;
        if (!(commandSender instanceof Player)) return false;
        Player p = Bukkit.getPlayer(commandSender.getName());
        ForceItemBattle.getBackpack().openBackpack(p);
        return false;
    }
}
