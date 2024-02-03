package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.FinishInventory;
import forceitembattle.util.ForceItem;
import forceitembattle.util.ForceItemPlayer;
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

    private ForceItemBattle forceItemBattle;
    public int place;

    public CommandResult(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("result").setExecutor(this);

        this.place = -1;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (this.forceItemBattle.getTimer().getTime() > 0) return false;
        if (!(commandSender instanceof Player player)) return false;
        if(args.length == 0) {
            if(player.isOp()) {
                if(this.forceItemBattle.getGamemanager().forceItemPlayerMap().isEmpty() || this.place == 0) {
                    player.sendMessage("No more players left.");
                    return false;
                }

                Map<UUID, ForceItemPlayer> sortedMapDesc = this.forceItemBattle.getGamemanager().sortByValue(this.forceItemBattle.getGamemanager().forceItemPlayerMap(), false);
                if(this.place == -1) this.place = sortedMapDesc.size();
                UUID uuid = (UUID) sortedMapDesc.keySet().toArray()[this.place - 1];

                Bukkit.getOnlinePlayers().forEach(players -> {
                    new FinishInventory(this.forceItemBattle, this.forceItemBattle.getGamemanager().getForceItemPlayer(uuid), this.place, true).open(players);
                });
                this.place--;
            }

        } else if (args.length == 1) {
            player.sendMessage("Currently 'in Arbeit' cuz lazy shit");
            //this.forceItemBattle.getGamemanager().forceItemPlayerMap().forEach((uuid, forceItemPlayer) -> player.sendMessage(uuid + " - " + forceItemPlayer.uuid()));
            new FinishInventory(this.forceItemBattle, this.forceItemBattle.getGamemanager().getForceItemPlayer(UUID.fromString(args[0])), null, false).open(player);
            return true;
        }


        return false;
    }
}
