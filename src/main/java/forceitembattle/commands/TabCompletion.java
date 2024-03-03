package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.PlayerStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TabCompletion implements TabCompleter {

    private final ForceItemBattle forceItemBattle;

    public TabCompletion(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (command.getName().equalsIgnoreCase("start") && strings.length == 1) {
            if (commandSender instanceof Player) {
                return new ArrayList<>(this.forceItemBattle.getSettings().gamePresetMap().keySet());
            }

        } else if (command.getName().equalsIgnoreCase("top") && strings.length == 1 && (commandSender instanceof Player)) {
            return Arrays.stream(PlayerStat.values()).filter(PlayerStat::isInLeaderboard).map(stat -> stat.name().toLowerCase()).toList();

        }

        return Collections.emptyList();
    }
}
