package forceitembattle.listener;

import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.manager.AntimatterPortalManager;
import forceitembattle.manager.ScatterDestinations;
import forceitembattle.service.FIBServiceClient;
import forceitembattle.settings.GameSettings;
import forceitembattle.event.AntimatterTeleporterUseEvent;
import forceitembattle.model.Dimension;
import forceitembattle.model.Landing;
import forceitembattle.settings.GameSetting;
import forceitembattle.service.PlayerCounter;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class PortalListener implements Listener {
    private final Roster roster;
    private final AntimatterPortalManager antimatterPortalManager;
    private final FIBServiceClient fibService;
    private final RoundPhase roundPhase;
    private final GameSettings settings;
    /**
     * Where each player's scatters have already sent them. The rule and the memory live there; this
     * listener grounds a destination and moves the player, which is the half that needs a world.
     */
    private final ScatterDestinations destinations;

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();

        if (!this.roundPhase.roundRunning() && !this.roundPhase.isEndGame()) {
            return;
        }
        Location playerLocation = player.getLocation();
        Collection<ArmorStand> armorStands = playerLocation.getWorld().getEntitiesByClass(ArmorStand.class);
        for (ArmorStand armorStand : armorStands) {
            ItemStack helmet = armorStand.getEquipment().getHelmet();
            if (helmet != null && helmet.getType() == Material.SNOWBALL) {
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
        if (!this.roundPhase.roundRunning()) {
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
        boolean midGame = this.roundPhase.roundRunning();

        if (midGame && this.settings.isSettingEnabled(GameSetting.STATS)) {
            ForceItemPlayer fip = this.roster.get(player.getUniqueId());
            this.fibService.statisticsWrites().recordPlayerCounter(
                    player.getUniqueId(), fip, PlayerCounter.ANTIMATTER_TELEPORTER_ENTRIES, 1);
        }

        Location origin = player.getLocation();
        Optional<Location> existing =
                this.destinations.existingTeleporterDestination(player.getUniqueId(), origin);
        if (existing.isPresent()) {
            if (midGame) {
                Bukkit.getPluginManager().callEvent(new AntimatterTeleporterUseEvent(player, false));
            }
            player.teleport(existing.get());
            return;
        }

        Location newLocation = ground(this.destinations.scatterTargetFrom(origin));
        layFloorUnder(newLocation);
        this.destinations.rememberTeleporter(player.getUniqueId(), origin, newLocation);

        // Unused before this round, so it counts as distinct.
        if (midGame) {
            Bukkit.getPluginManager().callEvent(new AntimatterTeleporterUseEvent(player, true));
        }

        player.teleport(newLocation);
    }

    /** Drops a scatter target onto the highest block at its column. */
    private static Location ground(Location target) {
        target.setY(target.getWorld().getHighestBlockYAt(target) + 1);
        return target;
    }

    /** So a scatter onto water, lava or air does not drown or drop whoever arrives. */
    private static void layFloorUnder(Location location) {
        Block block = location.clone().subtract(0, 1, 0).getBlock();
        if (Landing.needsFloor(block.getType())) {
            block.setType(Material.STONE);
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!this.roundPhase.roundRunning() && !this.roundPhase.isEndGame()) {
            return;
        }
        Player player = event.getPlayer();

        if (this.antimatterPortalManager.isAntimatterWorld(player.getWorld())) {
            return;
        }

        if (Dimension.of(player) != Dimension.END) {
            return;
        }

        Optional<Location> existing = this.destinations.existingEndDestination(player.getUniqueId());
        if (existing.isPresent()) {
            player.teleport(existing.get());
            return;
        }

        Location newLocation = ground(this.destinations.scatterTargetFrom(player.getLocation()));
        this.destinations.rememberEnd(player.getUniqueId(), newLocation);

        player.teleport(newLocation);
    }
}
