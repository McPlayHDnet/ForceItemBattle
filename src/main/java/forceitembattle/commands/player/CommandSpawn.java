package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class CommandSpawn extends CustomCommand {

    private final Supplier<Location> spawnLocation;

    public CommandSpawn(Supplier<Location> spawnLocation) {
        super("spawn");
        this.spawnLocation = spawnLocation;
        setDescription("Teleport to the spawn location");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.spawnLocation.get() == null) {
            player.sendMessage(Text.of("<red>The spawn location has not been set yet."));
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(this.spawnLocation.get());

        passengers.forEach(player::addPassenger);
    }
}
