package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandSpawn extends CustomCommand {

    public CommandSpawn() {
        super("spawn");
        setDescription("Teleport to the spawn location");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSpawnLocation() == null) {
            player.sendMessage("§cThe spawn location has not been set yet.");
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(this.plugin.getSpawnLocation());

        passengers.forEach(player::addPassenger);
    }
}
