package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandShout extends CustomCommand {

    public CommandShout() {
        super("shout");
        setDescription("Send global message when team chat is enabled");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold>" + player.getName() + " <dark_gray>» <white>" + String.join(" ", args)));

    }
}
