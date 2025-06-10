package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;

public class CommandReset extends CustomCommand {

    public CommandReset() {
        super("reset");

        setDescription("Restart server with new seed");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if(player.isOp()) {
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kick(this.plugin.getGamemanager().getMiniMessage().deserialize("<dark_red>Server Reset")));

            try {
                this.plugin.resetFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Bukkit.restart();
        }
    }
}
