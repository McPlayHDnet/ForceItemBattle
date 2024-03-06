package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.InvSettings;
import org.bukkit.entity.Player;

public class CommandSettings extends CustomCommand {

    public CommandSettings() {
        super("settings");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        new InvSettings(this.plugin, null).open(player);
    }
}
