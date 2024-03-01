package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandPing implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandPing(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("ping").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        player.sendMessage("§aYour ping: §e" + player.getPing() + "ms");

        return false;
    }
}
