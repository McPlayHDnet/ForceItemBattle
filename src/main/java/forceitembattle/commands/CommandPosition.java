package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandPosition implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandPosition(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("pos").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;
        if(!this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) return false;

        if(strings.length >= 1) {
            StringBuilder positionName = new StringBuilder();
            for (String string : strings) {
                positionName.append(string).append(" ");
            }

            if(this.forceItemBattle.getPositionManager().positionExist(positionName.toString())) {
                Location positionLocation = this.forceItemBattle.getPositionManager().getPosition(positionName.toString());
                player.sendMessage("§8» §6Position §8┃ §3" + positionName + "§7located at §3" + (int)positionLocation.getX() + "§7, §3" + (int)positionLocation.getY() + "§7, §3" + (int)positionLocation.getZ());
                return false;
            }
            Location playerLocation = player.getLocation();
            this.forceItemBattle.getPositionManager().createPosition(positionName.toString(), playerLocation);
            Bukkit.broadcastMessage("§8» §6Position §8┃ §3" + positionName + "§7located at §3" + (int)playerLocation.getX() + "§7, §3" + (int)playerLocation.getY() + "§7, §3" + (int)playerLocation.getZ());
        }

        return false;
    }
}
