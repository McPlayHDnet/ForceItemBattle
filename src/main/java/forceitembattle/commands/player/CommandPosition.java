package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CommandPosition extends CustomCommand implements CustomTabCompleter {

    private static final String prefix = "<dark_gray>» <gold>Position <dark_gray>┃ ";

    public CommandPosition(ForceItemBattle plugin) {
        super(plugin, "pos");
        setDescription("Add or show saved positions for structures");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT) && !player.isOp()) {
            player.sendMessage(Text.of("<red>You don't have permission to use this command."));
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.POSITIONS)) {
            player.sendMessage(Text.of("<red>Positions are disabled in this round!"));
            return;
        }

        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        if (forceItemPlayer.isSpectator()) return;

        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            Scheduler.runAsync(() -> sendAllPositions(player)); // Async because Location#distance takes some time.
            return;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            removePosition(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            return;
        }

        String positionName = String.join(" ", args);
        if (this.plugin.getPositionManager().positionExist(positionName)) {
            Scheduler.runAsync(() -> showPosition(player, positionName));
            return;
        }

        addNewPosition(player, positionName);
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return new ArrayList<>(this.plugin.getPositionManager().getAllPositions().keySet());
    }

    private void addNewPosition(Player player, String positionName) {
        Location playerLocation = player.getLocation();
        this.plugin.getPositionManager().createPosition(positionName, playerLocation);
        Bukkit.broadcast(Text.of(
                prefix + "<green>" + player.getName() + " <gray>added location of <dark_aqua>" + positionName
                        + " <gray>at " + locationToString(playerLocation)
                        + " <gray>in the " + Dimension.of(playerLocation.getWorld()).coloredName()
        ));
    }

    private void showPosition(Player player, String positionName) {
        Location positionLocation = this.plugin.getPositionManager().getPosition(positionName);
        player.sendMessage(Text.of(
                prefix + "<dark_aqua>" + positionName + " <gray>located at " + locationToString(positionLocation) + distance(player.getLocation(), positionLocation)
        ));
        this.plugin.getPositionManager().playParticleLine(player, positionLocation, Color.LIME);
    }

    private void sendAllPositions(Player player) {
        if (this.plugin.getPositionManager().getAllPositions().isEmpty()) {
            player.sendMessage(Text.of(prefix + "<gray>Nobody added any locations yet."));
            return;
        }

        player.sendMessage(Text.of(prefix + "<white>All saved locations"));
        this.plugin.getPositionManager().getAllPositions().forEach((name, location) -> {
            player.sendMessage(Text.of("<dark_gray>» <dark_aqua>" + name + " <gray>located at " + locationToString(location) + distance(player.getLocation(), location)));
        });
    }

    private void removePosition(Player player, String locationName) {
        if (!player.hasPermission("forceitembattle.position.remove")) {
            player.sendMessage(Text.of(prefix + "<red>You do not have permission to use this."));
            return;
        }

        if (locationName.equalsIgnoreCase("all")) {
            this.plugin.getPositionManager().clearPositions();
            player.sendMessage(Text.of(prefix + "<gray>All locations have been removed."));
            return;
        }

        if (!this.plugin.getPositionManager().positionExist(locationName)) {
            player.sendMessage(Text.of(prefix + "<red>Position <white>" + locationName + " <red>does not exist."));
            return;
        }

        this.plugin.getPositionManager().removePosition(locationName);
        player.sendMessage(Text.of(prefix + "<gray>Position <dark_aqua>" + locationName + " <gray>has been removed."));
    }

    // Utility stuff

    private String locationToString(Location location) {
        if (location.getWorld() == null) {
            return "<red>unknown location";
        }

        return "<dark_aqua>" + location.getBlockX() + "<gray>, <dark_aqua>" + location.getBlockY() + "<gray>, <dark_aqua>" + location.getBlockZ();
    }

    private String distance(Location playerLocation, Location destination) {
        if (playerLocation.getWorld() == null || destination.getWorld() == null) {
            return " <red>(unknown)";
        }

        if (!playerLocation.getWorld().equals(destination.getWorld())) {
            return " <gray>in the " + Dimension.of(destination.getWorld()).coloredName();
        }


        return " <green>(" + (int) playerLocation.distance(destination) + " blocks away)";
    }
}
