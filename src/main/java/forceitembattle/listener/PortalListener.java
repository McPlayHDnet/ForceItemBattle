package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@RequiredArgsConstructor
public class PortalListener implements Listener {

    private final ForceItemBattle plugin;

    private final Map<UUID, List<TeleporterLocation>> playerTeleporterLocations = new HashMap<>();
    private final Map<UUID, Location> playerEndLocations = new HashMap<>();

    private final Random random = new Random();

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();

        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        Location playerLocation = player.getLocation();
        Collection<ArmorStand> armorStands = playerLocation.getWorld().getEntitiesByClass(ArmorStand.class);
        for (ArmorStand armorStand : armorStands) {
            if (armorStand.getEquipment().getHelmet() != null && armorStand.getEquipment().getHelmet().getType() == Material.SNOWBALL) {
                Location armorStandLocation = armorStand.getLocation();

                double distanceSquared = playerLocation.distanceSquared(armorStandLocation);
                double detectionRangeSquared = 1.0;

                if (distanceSquared <= detectionRangeSquared) {
                    teleportPlayerRandomly(player);
                    return;
                }
            }
        }
    }

    private void teleportPlayerRandomly(Player player) {
        Location existingLocation = this.findExistingLocation(player);
        if (existingLocation != null) {
            player.teleport(existingLocation);
            return;
        }

        World world = player.getWorld();

        int xOffset = random.nextBoolean() ? random.nextInt(5001) + 5000 : -(random.nextInt(5001) + 5000);
        int zOffset = random.nextBoolean() ? random.nextInt(5001) + 5000 : -(random.nextInt(5001) + 5000);

        Location currentLocation = player.getLocation();
        Location newLocation = new Location(world, currentLocation.getX() + xOffset, currentLocation.getY(), currentLocation.getZ() + zOffset);
        newLocation.setY(world.getHighestBlockYAt(newLocation) + 1);

        Location blockLocation = newLocation.clone().subtract(0, 1, 0);
        Block block = blockLocation.getBlock();
        if (!block.getType().isBlock()) {
            block.setType(Material.STONE);
        }

        playerTeleporterLocations.get(player.getUniqueId()).add(new TeleporterLocation(currentLocation, newLocation));
        player.teleport(newLocation);
    }

    @Nullable
    private Location findExistingLocation(Player player) {
        Location playerLocation = player.getLocation();

        for (TeleporterLocation teleporterLocation : playerTeleporterLocations.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())) {
            if (teleporterLocation.isClose(playerLocation)) {
                return teleporterLocation.portalLocation;
            }
        }
        return null;
    }

    private record TeleporterLocation (Location portalLocation, Location destinationLocation) {

        public boolean isClose(Location location) {
            return portalLocation.distanceSquared(location) <= 625;
        }
    }


    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.playerEndLocations.containsKey(player.getUniqueId())) {
            player.teleport(this.playerEndLocations.get(player.getUniqueId()));
            return;
        }

        if (player.getWorld().getName().equals("world_the_end")) {
            Location spawnLocation = player.getLocation();

            int xOffset = random.nextBoolean() ? random.nextInt(10_001) + 5000 : -(random.nextInt(10_001) + 5000);
            int zOffset = random.nextBoolean() ? random.nextInt(10_001) + 5000 : -(random.nextInt(10_001) + 5000);

            Location currentLocation = player.getLocation();
            Location newLocation = new Location(player.getWorld(), currentLocation.getX() + xOffset, currentLocation.getY(), currentLocation.getZ() + zOffset);
            newLocation.setY(player.getWorld().getHighestBlockYAt(newLocation) + 1);
            playerEndLocations.put(player.getUniqueId(), newLocation);

            player.teleport(spawnLocation);
        }
    }
}
