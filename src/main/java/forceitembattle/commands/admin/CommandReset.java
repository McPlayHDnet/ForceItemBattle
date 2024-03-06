package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandReset extends CustomCommand {

    public CommandReset() {
        super("reset");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kickPlayer(ChatColor.DARK_RED + "Server Reset"));

        this.forceItemBattle.getConfig().set("isReset" , true);
        this.forceItemBattle.saveConfig();
        Bukkit.spigot().restart();
    }
}
