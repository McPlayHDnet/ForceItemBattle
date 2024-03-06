package forceitembattle.commands;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandSpawn extends CustomCommand {

    public CommandSpawn() {
        super("spawn");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.forceItemBattle.getSpawnLocation() == null) {
            player.sendMessage("§cThe spawn location has not been set yet.");
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(this.forceItemBattle.getSpawnLocation());

        passengers.forEach(player::addPassenger);
    }
}
