package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandReset implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandReset(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("reset").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Bukkit.getOnlinePlayers().forEach(player -> player.kickPlayer(ChatColor.DARK_RED + "Server Reset"));

        this.forceItemBattle.getConfig().set("isReset", true);
        this.forceItemBattle.saveConfig();
        Bukkit.spigot().restart();
        return false;
    }
}
