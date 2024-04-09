package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.entity.Player;

public class CommandPing extends CustomCommand {

    public CommandPing() {
        super("ping");
        setDescription("Check your ping");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>Your ping: <yellow>" + player.getPing() + "ms"));

    }
}
