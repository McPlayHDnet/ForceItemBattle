package forceitembattle.commands;

import org.bukkit.entity.Player;

public class CommandPing extends CustomCommand {

    public CommandPing() {
        super("ping");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        player.sendMessage("§aYour ping: §e" + player.getPing() + "ms");

    }
}
