package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.InvSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSettings implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandSettings(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("settings").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player player) {
            new InvSettings(this.forceItemBattle, null).open(player);
        }
        return false;
    }
}
