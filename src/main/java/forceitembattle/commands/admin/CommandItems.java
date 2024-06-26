package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.ItemsInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CommandItems extends CustomCommand {

    public CommandItems() {
        super("items");

        setDescription("Show all available items");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if(player.isOp()) {
            new ItemsInventory(this.plugin, player).open(player);
        }
    }
}
