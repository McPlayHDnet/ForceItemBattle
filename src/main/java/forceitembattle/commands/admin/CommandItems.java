package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import forceitembattle.commands.Precondition;
import java.util.List;
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
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        new ItemsInventory(this.plugin, player).open(player);
    }
}
