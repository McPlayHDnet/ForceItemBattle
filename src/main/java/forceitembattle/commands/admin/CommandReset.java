package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandReset extends CustomCommand {

    public CommandReset(ForceItemBattle plugin) {
        super(plugin, "reset");

        setDescription("Restart server with new seed");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) return;

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.kick(Text.of(
                "<dark_gray>» <gold><b>ForceItemBattle</b> <dark_gray>«" +
                        "\n" +
                        "<red>The world is being reset!" +
                        "\n"
        )));

        this.plugin.scheduleReset();
    }
}
