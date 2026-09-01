package forceitembattle.listener;

import forceitembattle.manager.Gamemanager;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.ProtectionManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ProtectionVerdict;
import forceitembattle.util.AdminNotifier;
import forceitembattle.util.Text;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Adapter for the protection rules: cancels what {@link ProtectionManager} refuses, plays the
 * refusal sound, and tells the operators.
 *
 * <p>Nothing here decides anything: every handler asks a question, and the only judgement it makes is
 * how to word the answer.
 */
@RequiredArgsConstructor
public class ProtectionListener implements Listener {
    /** Still needed for {@code new NamespacedKey(plugin, ...)}. */
    private final ForceItemBattle plugin;
    private final Roster roster;
    private final RoundPhase roundPhase;
    private final ProtectionManager protectionManager;
    private final List<CreatureSpawnEvent.SpawnReason> blockedSpawnReasons = List.of(
            CreatureSpawnEvent.SpawnReason.BUILD_WITHER
    );

    private final AdminNotifier notifier = new AdminNotifier();

    private ProtectionManager protection() {
        return this.protectionManager;
    }

    /**
     * Protection applies for as long as the round does, <b>pause included</b>. A pause stops this
     * plugin's clock and freezes the players; it does not stop the world. Asking {@code roundRunning}
     * here switches off every gate below the moment someone types {@code /pause}, and primed TNT,
     * lava, fire and pistons all start working again.
     */
    private boolean roundInProgress() {
        return this.roundPhase.roundInProgress();
    }

    private ForceItemPlayer forceItemPlayer(Player player) {
        return this.roster.get(player.getUniqueId());
    }

    @EventHandler
    public void onBlockEntitySpawn(CreatureSpawnEvent e) {
        if (blockedSpawnReasons.contains(e.getSpawnReason())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!this.roundInProgress()) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionVerdict verdict = this.protection().mayBreak(player, this.forceItemPlayer(player), block);

        if (verdict.denied()) {
            this.refuse(event, player, switch (verdict) {
                case NEAR_BED -> "break a block near bed";
                default -> "break container";
            }, block.getLocation());
            return;
        }

        // Harmless on a block that never was a container, and it keeps the ownership map from
        // holding entries for blocks that are gone.
        this.protection().breakContainer(block);
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!this.roundInProgress()) {
            return;
        }

        Location inventoryLocation = event.getInventory().getLocation();
        if (inventoryLocation == null) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Block block = inventoryLocation.getBlock();

        if (!this.protection().canBreakContainer(this.forceItemPlayer(player), block)) {
            this.refuse(event, player, "open a container", block.getLocation());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Gamemanager.isJoker(event.getItemInHand())) {
            event.setCancelled(true);
            return;
        }
        if (!this.roundInProgress()) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        ForceItemPlayer placer = this.forceItemPlayer(player);
        ProtectionVerdict verdict = this.protection().mayPlace(player, placer, block);

        if (verdict.denied()) {
            this.refuse(event, player, switch (verdict) {
                case NEAR_BED -> "place a block near bed";
                default -> "place a hopper below a container";
            }, block.getLocation());
            return;
        }

        if (block.getState() instanceof Container) {
            this.protection().protectContainer(placer, block);
        }
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        if (!this.roundInProgress()) {
            return;
        }

        e.setCancelled(true);

        Location location = e.getBlock().getLocation();
        for (Player player : this.protection().witnesses(location)) {
            player.sendMessage(Text.of("<red>Pistons are disabled."));
        }

        this.notifier.notifyOps("<red>" + this.protection().witnessNames(location)
                + " <gray> near an extending piston at <white>" + format(location));
    }

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent event) {
        if (!this.roundInProgress()) {
            return;
        }

        if (event.blockList().removeIf(this.protection()::isProtectedFromNature)) {
            this.reportExplosion(event.getLocation());
        }
    }

    @EventHandler
    public void onEntityExplode(BlockExplodeEvent event) {
        if (!this.roundInProgress()) {
            return;
        }

        if (event.blockList().removeIf(this.protection()::isProtectedFromNature)) {
            this.reportExplosion(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onLavaSpread(BlockFromToEvent e) {
        if (e.getBlock().getType() != Material.LAVA) {
            return;
        }

        if (this.roundInProgress() && this.protection().isProtectedFromNature(e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLavaPlace(PlayerBucketEmptyEvent e) {
        if (e.getBucket() != Material.LAVA_BUCKET) {
            return;
        }

        if (this.roundInProgress() && this.protection().isProtectedFromNature(e.getBlockClicked())) {
            this.refuse(e, e.getPlayer(), "place a lava bucket near protected block",
                    e.getBlockClicked().getLocation());
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        if (this.roundInProgress() && this.protection().isProtectedFromNature(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    /** Stop it, tell the player with a sound, tell the operators what was attempted. */
    private void refuse(Cancellable event, Player player, String attempt, Location location) {
        event.setCancelled(true);
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
        this.notifier.notifyOps("<red>" + player.getName() + " <gray>tried to " + attempt
                + " at <white>" + format(location));
    }

    private void reportExplosion(Location location) {
        this.notifier.notifyOps("<red>explosion <gray>tried to break protected blocks at <white>"
                + format(location) + " <gray>[nearby: " + this.protection().witnessNames(location) + "]");
    }

    private static String format(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
}
