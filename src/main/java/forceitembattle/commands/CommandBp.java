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

        if(commandSender instanceof Player player) {
            if (ForceItemBattle.getTimer().isRunning()) {
                if(ForceItemBattle.getInstance().getConfig().getBoolean("settings.backpack")) {
                    ForceItemBattle.getBackpack().openPlayerBackpack(player);
                } else {
                    player.sendMessage("§cBackpacks are disabled in this round!");
                }
            } else {
                player.sendMessage("§cThe game has not started yet!");
            }
        }

        return false;
    }
}
