package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.service.FibStatisticsClient;
import forceitembattle.service.PlayerStatsWrite;
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

    private final ForceItemBattle plugin;

    private final Map<UUID, List<TeleporterLocation>> playerTeleporterLocations = new HashMap<>();
    private final Map<UUID, Location> playerEndLocations = new HashMap<>();

    private final Random random = new Random();

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();

        if (!this.plugin.getGamemanager().isMidGame() && !this.plugin.getGamemanager().isEndGame()) {
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
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.HARD)) {
            player.sendMessage(Text.of("<red>Travelling to other dimensions is disabled!"));
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            playerPortalEvent.setCanCreatePortal(false);
            playerPortalEvent.setCancelled(true);
        }
    }

    private void teleportPlayerRandomly(Player player) {
        boolean midGame = this.plugin.getGamemanager().isMidGame();

        if (midGame && this.plugin.getSettings().isSettingEnabled(GameSetting.STATS)) {
            FibStatisticsClient helper = this.plugin.getFibService().statistics();
            ForceItemPlayer fip = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
            PlayerStatsWrite.record(helper, player.getUniqueId(), fip,
                    () -> FIBServiceClient.soloUpdate().enteredAntimatterTeleporterAdd(1L),
                    () -> FIBServiceClient.memberUpdate().enteredAntimatterTeleporterAdd(1L));
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
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }
        Player player = event.getPlayer();

        if (this.plugin.getAntimatterPortalManager().isAntimatterWorld(player.getWorld())) {
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
