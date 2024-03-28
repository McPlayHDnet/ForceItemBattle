package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandReset extends CustomCommand {

    public CommandReset() {
        super("reset");

        setDescription("Restart server with new seed");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kickPlayer(ChatColor.DARK_RED + "Server Reset"));

        this.plugin.getConfig().set("isReset" , true);
        this.plugin.saveConfig();
        Bukkit.spigot().restart();
    }
}
