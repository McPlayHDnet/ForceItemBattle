package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBed extends CustomCommand {

    public CommandBed() {
        super("bed");
        setDescription("Teleport to your bed location");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (player.getRespawnLocation() == null) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You don't have a bed respawn point."));
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(player.getRespawnLocation());

        passengers.forEach(player::addPassenger);
    }
}
