package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.gui.SettingsInventory;
import org.bukkit.entity.Player;

public class CommandSettings extends CustomCommand {

    public CommandSettings(ForceItemBattle plugin) {
        super(plugin, "settings");

        setDescription("Manage settings");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) return;
        new SettingsInventory(this.plugin, null).open(player);
    }
}
