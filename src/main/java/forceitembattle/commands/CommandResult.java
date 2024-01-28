package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.FinishInventory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CommandResult implements CommandExecutor {

    public static int place = -1;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (ForceItemBattle.getTimer().isRunning()) return false;
        if (!(commandSender instanceof Player player)) return false;
        if(player.isOp()) {

            if(args.length == 0) {
                if(ForceItemBattle.getGamemanager().getScore().isEmpty() || place == 0) {
                    player.sendMessage("No more players left.");
                    return false;
                }

                Map<UUID, Integer> sortedMapDesc = ForceItemBattle.getGamemanager().sortByValue(ForceItemBattle.getGamemanager().getScore(), false);
                UUID uuid = (UUID) sortedMapDesc.keySet().toArray()[sortedMapDesc.size() - 1];

                if(place == -1) place = sortedMapDesc.size();

                Bukkit.getOnlinePlayers().forEach(players -> new FinishInventory(Objects.requireNonNull(Bukkit.getPlayer(uuid)), place, true).open(players));
                place--;
            } else if (args.length == 1) {
                if (Bukkit.getPlayer(args[0]) != null) {
                    player.sendMessage("Currently 'in Arbeit' cuz lazy shit");
                    //new FinishInventory(Objects.requireNonNull(Bukkit.getPlayer(args[0])), null, false).open(player);
                } else {
                    commandSender.sendMessage(ChatColor.RED + "This player is not online");
                }
                return true;
            }

        }
        return false;
    }
}
