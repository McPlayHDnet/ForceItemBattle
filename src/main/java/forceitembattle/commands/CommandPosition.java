package forceitembattle.commands;

import forceitembattle.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandPosition extends CustomCommand {

    public CommandPosition() {
        super("pos");
    }

    private static final String prefix = "§8» §6Position §8┃ ";

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
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
        if (this.forceItemBattle.getPositionManager().positionExist(positionName)) {
            Scheduler.runAsync(() -> showPosition(player, positionName));
            return;
        }

        addNewPosition(player, positionName);
    }

    private void addNewPosition(Player player, String positionName) {
        Location playerLocation = player.getLocation();
        this.forceItemBattle.getPositionManager().createPosition(positionName, playerLocation);
        Bukkit.broadcastMessage(
                prefix + "§a" + player.getName() + " §7added location of §3" + positionName + "§7 at " + locationToString(playerLocation) + " §7in the " + getWorldName(playerLocation.getWorld())
        );
    }

    private void showPosition(Player player, String positionName) {
        Location positionLocation = this.forceItemBattle.getPositionManager().getPosition(positionName);
        player.sendMessage(
                prefix + "§3" + positionName + " §7located at " + locationToString(positionLocation) + distance(player.getLocation(), positionLocation)
        );
    }

    private void sendAllPositions(Player player) {
        if (this.forceItemBattle.getPositionManager().getAllPositions().isEmpty()) {
            player.sendMessage(prefix + "§7Nobody added any locations yet.");
            return;
        }

        player.sendMessage(prefix + "§fAll saved locations");
        this.forceItemBattle.getPositionManager().getAllPositions().forEach((name, location) -> {
            player.sendMessage("§8» §3" + name + " §7located at " + locationToString(location) + distance(player.getLocation(), location));
        });
    }

    private void removePosition(Player player, String locationName) {
        if (!player.hasPermission("forceitembattle.position.remove")) {
            player.sendMessage(prefix + "§cYou do not have permission to use this.");
            return;
        }

        if (locationName.equalsIgnoreCase("all")) {
            this.forceItemBattle.getPositionManager().clearPositions();
            player.sendMessage(prefix + "§7All locations have been removed.");
            return;
        }

        if (!this.forceItemBattle.getPositionManager().positionExist(locationName)) {
            player.sendMessage(prefix + "§cPosition §f" + locationName + " §cdoes not exist.");
            return;
        }

        this.forceItemBattle.getPositionManager().removePosition(locationName);
        player.sendMessage(prefix + "§7Position §3" + locationName + " §7has been removed.");
    }

    // Utility stuff

    private String locationToString(Location location) {
        if (location.getWorld() == null) {
            return "§cunknown location";
        }

        return "§3" + location.getBlockX() + "§7, §3" + location.getBlockY() + "§7, §3" + location.getBlockZ();
    }

    private String distance(Location playerLocation, Location destination) {
        if (playerLocation.getWorld() == null || destination.getWorld() == null) {
            return " §c(unknown)";
        }

        if (!playerLocation.getWorld().equals(destination.getWorld())) {
            return " §7in the " + getWorldName(destination.getWorld());
        }


        return " §a(" + (int) playerLocation.distance(destination) + " blocks away)";
    }

    private String getWorldName(World world) {
        if (world == null) {
            return "§8unknown";
        }

        String worldName = world.getName();

        if (worldName.contains("nether")) {
            return "§cnether";
        }

        if (worldName.contains("end")) {
            return "§eend";
        }

        return "§aoverworld";
    }
}
