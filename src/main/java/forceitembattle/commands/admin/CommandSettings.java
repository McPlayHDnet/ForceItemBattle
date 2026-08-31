package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.gui.SettingsInventory;
import org.bukkit.entity.Player;

public final class CommandSettings extends CustomCommand {

    public CommandSettings(ForceItemBattle plugin) {
        super(plugin, "settings");

        setDescription("Manage settings");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        new SettingsInventory(this.plugin, null).open(player);
    }
}
