package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.PlayerStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        } else if(command.getName().equalsIgnoreCase("top") && strings.length == 1) {
            if(commandSender instanceof Player player) {
                return Arrays.stream(PlayerStat.values()).filter(PlayerStat::isInLeaderboard).map(stat -> stat.name().toLowerCase()).collect(Collectors.toList());
            }
        }

        return null;
    }
}
