package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandSpawn implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandSpawn(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("spawn").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if (this.forceItemBattle.getSpawnLocation() == null) {
            player.sendMessage("§cThe spawn location has not been set yet.");
            return false;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(this.forceItemBattle.getSpawnLocation());

        passengers.forEach(player::addPassenger);
        return false;
    }
}
