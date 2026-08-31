package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.gui.ItemsInventory;
import org.bukkit.entity.Player;

public final class CommandItems extends CustomCommand {

    public CommandItems(ForceItemBattle plugin) {
        super(plugin, "items");

        setDescription("Show all available items");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) return;
        new ItemsInventory(this.plugin, player).open(player);
    }
}
