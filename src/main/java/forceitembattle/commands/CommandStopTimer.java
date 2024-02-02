package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandStopTimer implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandStopTimer(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("stoptimer").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) return false;
        if (!this.forceItemBattle.getGamemanager().isMidGame()) {
            commandSender.sendMessage(ChatColor.RED + "The game is not running. Start it first with /start");
            return false;
        }
        this.forceItemBattle.getTimer().setTime(1);
        return false;
    }
}
