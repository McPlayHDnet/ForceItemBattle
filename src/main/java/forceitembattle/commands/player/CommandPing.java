package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import org.bukkit.entity.Player;

public class CommandPing extends CustomCommand {

    public CommandPing(ForceItemBattle plugin) {
        super(plugin, "ping");
        setDescription("Check your ping");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<green>Your ping: <yellow>" + player.getPing() + "ms"));

    }
}
