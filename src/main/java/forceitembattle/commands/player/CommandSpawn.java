package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class CommandSpawn extends CustomCommand {

    public CommandSpawn(ForceItemBattle plugin) {
        super(plugin, "spawn");
        setDescription("Teleport to the spawn location");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSpawnLocation() == null) {
            player.sendMessage(Text.of("<red>The spawn location has not been set yet."));
            return;
        }

        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        passengers.forEach(player::removePassenger);

        player.teleport(this.plugin.getSpawnLocation());

        passengers.forEach(player::addPassenger);
    }
}
