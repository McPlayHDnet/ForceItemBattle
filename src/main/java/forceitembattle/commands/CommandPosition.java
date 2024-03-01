package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandPosition implements CommandExecutor {

    private ForceItemBattle plugin;

    public CommandPosition(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("pos").setExecutor(this);
    }

    private static final String prefix = "§8» §6Position §8┃ ";

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player player)) {
            return false;
        }
        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
            return false;
        }

        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            Scheduler.runAsync(() -> sendAllPositions(player)); // Async because Location#distance takes some time.
            return false;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            removePosition(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            return false;
        }

        String positionName = String.join(" ", args);
        if (this.plugin.getPositionManager().positionExist(positionName)) {
            Scheduler.runAsync(() -> showPosition(player, positionName));
            return false;
        }

        addNewPosition(player, positionName);
        return false;
    }

    private void addNewPosition(Player player, String positionName) {
        Location playerLocation = player.getLocation();
        this.plugin.getPositionManager().createPosition(positionName, playerLocation);
        Bukkit.broadcastMessage(
                prefix + "§a" + player.getName() + " §7added location of §3" + positionName + "§7 at " + locationToString(playerLocation)
        );
    }

    private void showPosition(Player player, String positionName) {
        Location positionLocation = this.plugin.getPositionManager().getPosition(positionName);
        player.sendMessage(
                prefix + "§3" + positionName + " §7located at " + locationToString(positionLocation) + distance(player.getLocation(), positionLocation)
        );
    }

    private void sendAllPositions(Player player) {
        if (this.plugin.getPositionManager().getAllPositions().isEmpty()) {
            player.sendMessage(prefix + "§7Nobody added any locations yet.");
            return;
        }

        player.sendMessage(prefix + "§fAll saved locations");
        this.plugin.getPositionManager().getAllPositions().forEach((name, location) -> {
            player.sendMessage("§8» §3" + name + " §7located at " + locationToString(location) + distance(player.getLocation(), location));
        });
    }

    private void removePosition(Player player, String locationName) {
        if (!player.hasPermission("forceitembattle.position.remove")) {
            player.sendMessage(prefix + "§cYou do not have permission to use this.");
            return;
        }

        if (locationName.equalsIgnoreCase("all")) {
            this.plugin.getPositionManager().clearPositions();
            player.sendMessage(prefix + "§7All locations have been removed.");
            return;
        }

        if (!this.plugin.getPositionManager().positionExist(locationName)) {
            player.sendMessage(prefix + "§cPosition §f" + locationName + " §cdoes not exist.");
            return;
        }

        this.plugin.getPositionManager().removePosition(locationName);
        player.sendMessage(prefix + "§7Position §3" + locationName + " §7has been removed.");
    }

    // Utility stuff

    private String locationToString(Location location) {
        return "§3" + location.getBlockX() + "§7, §3" + location.getBlockY() + "§7, §3" + location.getBlockZ();
    }

    private String distance(Location first, Location second) {
        return " §3(" + (int) first.distance(second) + " blocks away)";
    }
}
