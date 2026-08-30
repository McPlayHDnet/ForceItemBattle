package forceitembattle.listener;

import forceitembattle.manager.AntimatterPortalManager;
import forceitembattle.manager.Gamemanager;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.PlayerCounter;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class PortalListener implements Listener {
    private final AntimatterPortalManager antimatterPortalManager;
    private final FIBServiceClient fibService;
    private final Gamemanager gamemanager;
    private final GameSettings settings;
    private final Map<UUID, List<TeleporterLocation>> playerTeleporterLocations = new HashMap<>();
    private final Map<UUID, Location> playerEndLocations = new HashMap<>();

    private final Random random = new Random();

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();

        if (!this.gamemanager.roundRunning() && !this.gamemanager.isEndGame()) {
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

    // ignoreCancelled, so the antimatter Depths' return portal — which AntimatterPortalListener
    // cancels and handles itself — does not also draw a "travelling is disabled" refusal here.
    @EventHandler(ignoreCancelled = true)
    public void onPortalEvent(PlayerPortalEvent playerPortalEvent) {
        Player player = playerPortalEvent.getPlayer();
        if (!this.gamemanager.roundRunning()) {
            return;
        }

        if (!this.settings.isSettingEnabled(GameSetting.HARD)) {
            player.sendMessage(Text.of("<red>Travelling to other dimensions is disabled!"));
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            playerPortalEvent.setCanCreatePortal(false);
            playerPortalEvent.setCancelled(true);
        }
    }

    private void teleportPlayerRandomly(Player player) {
        boolean midGame = this.gamemanager.roundRunning();

        if (midGame && this.settings.isSettingEnabled(GameSetting.STATS)) {
            ForceItemPlayer fip = this.gamemanager.getForceItemPlayer(player.getUniqueId());
            this.fibService.statistics().recordPlayerCounter(
                    player.getUniqueId(), fip, PlayerCounter.ANTIMATTER_TELEPORTER_ENTRIES, 1);
        }

        Location existingLocation = this.findExistingLocation(player);
        if (existingLocation != null) {
            // Re-using a teleporter already used this round — not a new/distinct one.
            if (midGame) {
                Bukkit.getPluginManager().callEvent(new AntimatterTeleporterUseEvent(player, false));
            }
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

        // A teleporter this player hasn't used before this round — counts as distinct.
        if (midGame) {
            Bukkit.getPluginManager().callEvent(new AntimatterTeleporterUseEvent(player, true));
        }

        player.teleport(newLocation);
    }

    @Nullable
    private Location findExistingLocation(Player player) {
        Location playerLocation = player.getLocation();

        for (TeleporterLocation teleporterLocation : playerTeleporterLocations.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())) {
            if (teleporterLocation.isClose(playerLocation)) {
                return teleporterLocation.destinationLocation;
            }
        }
        return null;
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!this.gamemanager.roundRunning() && !this.gamemanager.isEndGame()) {
            return;
        }
        Player player = event.getPlayer();

        if (this.antimatterPortalManager.isAntimatterWorld(player.getWorld())) {
            return;
        }

        if (Dimension.of(player) == Dimension.END) {
            if (this.playerEndLocations.containsKey(player.getUniqueId())) {
                player.teleport(this.playerEndLocations.get(player.getUniqueId()));
                return;
            }

            int xOffset = random.nextBoolean() ? random.nextInt(10_001) + 5000 : -(random.nextInt(10_001) + 5000);
            int zOffset = random.nextBoolean() ? random.nextInt(10_001) + 5000 : -(random.nextInt(10_001) + 5000);

            Location currentLocation = player.getLocation();
            Location newLocation = new Location(player.getWorld(), currentLocation.getX() + xOffset, currentLocation.getY(), currentLocation.getZ() + zOffset);
            newLocation.setY(player.getWorld().getHighestBlockYAt(newLocation) + 1);
            playerEndLocations.put(player.getUniqueId(), newLocation);

            player.teleport(newLocation);
        }
    }

    private record TeleporterLocation(Location portalLocation, Location destinationLocation) {

        public boolean isClose(Location location) {
            return portalLocation.distanceSquared(location) <= 625;
        }
    }
}
