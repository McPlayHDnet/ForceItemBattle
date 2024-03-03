package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBed implements CommandExecutor {

    public CommandBed(ForceItemBattle forceItemBattle) {
        forceItemBattle.getCommand("bed").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if (player.getRespawnLocation() == null) {
            player.sendMessage("§cYou don't have a bed respawn point.");
            return false;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(player.getRespawnLocation());

        passengers.forEach(player::addPassenger);
        return false;
    }
}
