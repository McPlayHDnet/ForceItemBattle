package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.Gamemanager;
import forceitembattle.util.ForceItemPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ProtectionListener implements Listener {

    private final ForceItemBattle plugin;

    private final List<CreatureSpawnEvent.SpawnReason> blockedSpawnReasons = List.of(
            CreatureSpawnEvent.SpawnReason.BUILD_WITHER
    );

    @EventHandler
    public void onBlockEntitySpawn(CreatureSpawnEvent e) {
        if (blockedSpawnReasons.contains(e.getSpawnReason())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        if (!this.plugin.getGamemanager().isMidGame()) {
            event.setCancelled(true);
            return;
        }

        Block brokenBlock = event.getBlock();

        if (this.plugin.getProtectionManager().isNearProtectedBed(player, brokenBlock.getLocation())) {
            event.setCancelled(true);
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
            notify("<red>" + player.getName() + " <gray>tried to break a block near bed at <white>" + string(brokenBlock.getLocation()));
            return;
        }

        if (brokenBlock.getState() instanceof Container) {
            if (this.plugin.getProtectionManager().canBreakContainer(forceItemPlayer, event.getBlock())) {
                this.plugin.getProtectionManager().breakContainer(event.getBlock());
                return;
            }

            event.setCancelled(true);
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
            notify("<red>" + player.getName() + " <gray>tried to break container at <white>" + string(event.getBlock().getLocation()));
            return;
        }
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        if (!this.plugin.getGamemanager().isMidGame()) {
            return;
        }

        Location inventoryLocation = event.getInventory().getLocation();
        if (inventoryLocation == null) {
            return;
        }

        Block block = inventoryLocation.getBlock();
        if (this.plugin.getProtectionManager().canBreakContainer(forceItemPlayer, block)) {
            return;
        }

        event.setCancelled(true);
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
        notify("<red>" + player.getName() + " <gray>tried to open a container at <white>" + string(block.getLocation()));
        return;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Gamemanager.isJoker(event.getBlock().getType())) {
            event.setCancelled(true);
            return;
        }
        if (!this.plugin.getGamemanager().isMidGame()) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (this.plugin.getProtectionManager().isNearProtectedBed(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
            notify("<red>" + player.getName() + " <gray>tried to place a block near bed at <white>" + string(event.getBlock().getLocation()));
            return;
        }

        if (event.getBlock().getType() == Material.HOPPER) {
            if (!this.plugin.getProtectionManager().canBreakContainer(forceItemPlayer, event.getBlock().getRelative(BlockFace.UP))) {
                event.setCancelled(true);
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
                notify("<red>" + player.getName() + " <gray>tried to place a hopper below a container at <white>" + string(event.getBlock().getLocation()));
                return;
            }
        }

        if (event.getBlock().getState() instanceof Container) {
            this.plugin.getProtectionManager().protectContainer(forceItemPlayer, event.getBlock());
        }
        return;
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        if (this.plugin.getGamemanager().isMidGame()) {
            e.setCancelled(true);
            for (Player player : getPlayersNearby(e.getBlock().getLocation())) {
                player.sendMessage(plugin.getGamemanager().getMiniMessage().deserialize(
                        "<red>Pistons are disabled."
                ));
            }

            notify("<red>" + playersNearby(e.getBlock().getLocation()) + " <gray> near an extending piston at <white>" + string(e.getBlock().getLocation()));
        }
    }

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) {
            boolean removed = event.blockList().removeIf(this::isBlockProtected);

            if (removed) {
                notify("<red>explosion <gray>tried to break protected blocks at <white>" + string(event.getLocation()) + " <gray>[nearby: " + playersNearby(event.getLocation()) + "]");
            }
        }
    }

    @EventHandler
    public void onEntityExplode(BlockExplodeEvent event) {
        if (this.plugin.getGamemanager().isMidGame()) {
            boolean removed = event.blockList().removeIf(this::isBlockProtected);

            if (removed) {
                notify("<red>explosion <gray>tried to break protected blocks at <white>" + string(event.getBlock().getLocation()) + " <gray>[nearby: " + playersNearby(event.getBlock().getLocation()) + "]");
            }
        }
    }

    @EventHandler
    public void onLavaSpread(BlockFromToEvent e) {
        if (e.getBlock().getType() != Material.LAVA) {
            return;
        }

        if (this.plugin.getGamemanager().isMidGame()) {
            if (isBlockProtected(e.getToBlock())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLavaPlace(PlayerBucketEmptyEvent e) {
        if (e.getBucket() != Material.LAVA_BUCKET) {
            return;
        }

        if (this.plugin.getGamemanager().isMidGame()) {
            if (isBlockProtected(e.getBlockClicked())) {
                e.setCancelled(true);
                e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                notify("<red>" + e.getPlayer().getName() + " <gray>tried to place a lava bucket near protected block at <white>" + string(e.getBlockClicked().getLocation()));
            }
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        if (this.plugin.getGamemanager().isMidGame()) {
            if (isBlockProtected(e.getBlock())) {
                e.setCancelled(true);
            }
        }
    }

    // utils

    // store all notify messages to prevent spam
    private final List<String> sentMessages = new ArrayList<>();

    private void notify(String msg) {
        if (sentMessages.contains(msg)) {
            return;
        }
        sentMessages.add(msg);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(plugin.getGamemanager().getMiniMessage().deserialize(msg));
            }
        }
    }

    private String string(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private List<Player> getPlayersNearby(Location location) {
        List<Player> players = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(location.getWorld())) {
                continue;
            }

            if (player.getLocation().distanceSquared(location) < 225) {
                players.add(player);
            }
        }
        return players;
    }

    private String playersNearby(Location location) {
        StringBuilder builder = new StringBuilder();

        for (Player player : getPlayersNearby(location)) {
            builder.append(player.getName()).append(", ");
        }

        if (builder.isEmpty()) {
            return "nobody";
        }

        return builder.substring(0, builder.length() - 2);
    }

    private boolean isBlockProtected(Block block) {
        return !this.plugin.getProtectionManager().canBreakContainer(null, block)
                || this.plugin.getProtectionManager().isNearProtectedBed(null, block.getLocation());
    }


}
