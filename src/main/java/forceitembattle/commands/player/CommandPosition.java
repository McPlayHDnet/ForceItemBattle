package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.OP_WHEN_EVENT;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.PositionManager;
import forceitembattle.model.Dimension;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.LocationFormat;
import forceitembattle.util.Prefix;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class CommandPosition extends CustomCommand implements CustomTabCompleter {

    private final Roster roster;
    private final PositionManager positionManager;

    public CommandPosition(Roster roster, PositionManager positionManager) {
        super("pos");
        this.roster = roster;
        this.positionManager = positionManager;
        setDescription("Add or show saved positions for structures");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP_WHEN_EVENT,
                Precondition.setting(GameSetting.POSITIONS, "<red>Positions are disabled in this round!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        // Silently, as before: /pos from a spectator is a no-op rather than a refusal.
        if (this.roster.participant(player.getUniqueId()).isEmpty()) {
            return;
        }

        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            Scheduler.runAsync(() -> sendAllPositions(player)); // Async because Location#distance takes some time.
            return;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            removePosition(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            return;
        }

        String positionName = String.join(" ", args);
        if (this.positionManager.positionExist(positionName)) {
            Scheduler.runAsync(() -> showPosition(player, positionName));
            return;
        }

        addNewPosition(player, positionName);
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return new ArrayList<>(this.positionManager.getAllPositions().keySet());
    }

    private void addNewPosition(Player player, String positionName) {
        Location playerLocation = player.getLocation();
        this.positionManager.createPosition(positionName, playerLocation);
        Bukkit.broadcast(Text.of(
                Prefix.POSITION + "<green>" + player.getName() + " <gray>added location of <dark_aqua>" + positionName
                        + " <gray>at " + LocationFormat.xyz(playerLocation)
                        + " <gray>in the " + Dimension.of(playerLocation.getWorld()).coloredName()
        ));
    }

    private void showPosition(Player player, String positionName) {
        Location positionLocation = this.positionManager.getPosition(positionName);
        player.sendMessage(Text.of(
                Prefix.POSITION + "<dark_aqua>" + positionName + " <gray>located at "
                        + LocationFormat.xyz(positionLocation)
                        + LocationFormat.distance(player.getLocation(), positionLocation)
        ));
        this.positionManager.playParticleLine(player, positionLocation, Color.LIME);
    }

    private void sendAllPositions(Player player) {
        if (this.positionManager.getAllPositions().isEmpty()) {
            player.sendMessage(Text.of(Prefix.POSITION + "<gray>Nobody added any locations yet."));
            return;
        }

        player.sendMessage(Text.of(Prefix.POSITION + "<white>All saved locations"));
        this.positionManager.getAllPositions().forEach((name, location) -> {
            player.sendMessage(Text.of("<dark_gray>» <dark_aqua>" + name + " <gray>located at "
                    + LocationFormat.xyz(location)
                    + LocationFormat.distance(player.getLocation(), location)));
        });
    }

    private void removePosition(Player player, String locationName) {
        if (!player.hasPermission("forceitembattle.position.remove")) {
            player.sendMessage(Text.of(Prefix.POSITION + "<red>You do not have permission to use this."));
            return;
        }

        if (locationName.equalsIgnoreCase("all")) {
            this.positionManager.clearPositions();
            player.sendMessage(Text.of(Prefix.POSITION + "<gray>All locations have been removed."));
            return;
        }

        if (!this.positionManager.positionExist(locationName)) {
            player.sendMessage(Text.of(Prefix.POSITION + "<red>Position <white>" + locationName + " <red>does not exist."));
            return;
        }

        this.positionManager.removePosition(locationName);
        player.sendMessage(Text.of(Prefix.POSITION + "<gray>Position <dark_aqua>" + locationName + " <gray>has been removed."));
    }
}
