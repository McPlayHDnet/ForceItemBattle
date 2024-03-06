package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBed extends CustomCommand {

    public CommandBed() {
        super("bed");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (player.getRespawnLocation() == null) {
            player.sendMessage("§cYou don't have a bed respawn point.");
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(player.getRespawnLocation());

        passengers.forEach(player::addPassenger);
    }
}
