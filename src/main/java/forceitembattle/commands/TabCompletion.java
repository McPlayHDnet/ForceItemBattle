package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompletion implements TabCompleter {

    private ForceItemBattle forceItemBattle;

    public TabCompletion(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if(command.getName().equalsIgnoreCase("start") && strings.length == 1) {
            if(commandSender instanceof Player player) {
                return new ArrayList<>(this.forceItemBattle.getSettings().gamePresetMap().keySet());
            }
        }

        return null;
    }
}
