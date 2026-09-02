package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.gui.ItemsInventory;
import forceitembattle.manager.ItemDifficultiesManager;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandItems extends CustomCommand {

    private final ItemDifficultiesManager items;

    public CommandItems(ItemDifficultiesManager items) {
        super("items");
        this.items = items;

        setDescription("Show all available items");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        new ItemsInventory(this.items, player).open(player);
    }
}
