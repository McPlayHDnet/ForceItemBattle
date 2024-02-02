package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSkip implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandSkip(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("skip").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!this.forceItemBattle.getGamemanager().isMidGame()) {
            sender.sendMessage(ChatColor.RED + "The game is not running. Start it first with /start");
            return false;
        } else
        if (args.length == 1) {
            if (Bukkit.getPlayer(args[0]) != null) {
                player.sendMessage("§7You skipped this item");
                this.forceItemBattle.logToFile("[" + this.forceItemBattle.getTimer().getTime() + "] | " + args[0] + " skipped " + this.forceItemBattle.getGamemanager().getCurrentMaterial(Bukkit.getPlayer(args[0])));
                this.forceItemBattle.getGamemanager().skipItem(args[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "This player is not online");
            }
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /skip <player_name>");
            return false;
        }
    }
}
