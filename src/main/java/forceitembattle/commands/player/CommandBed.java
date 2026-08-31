package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class CommandBed extends CustomCommand {

    public CommandBed(ForceItemBattle plugin) {
        super(plugin, "bed");
        setDescription("Teleport to your bed location");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (player.getRespawnLocation() == null) {
            player.sendMessage(Text.of("<red>You don't have a bed respawn point."));
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(player.getRespawnLocation());

        passengers.forEach(player::addPassenger);
    }
}
