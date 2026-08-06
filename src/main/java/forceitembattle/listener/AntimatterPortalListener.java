package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.AntimatterPortalManager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.util.Text;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Turns a Totem of Antimatter placed in an Antimatter Depths portal vault into an open portal, and
 * walks its owner through when they step into it.
 *
 * <p>The vanilla vault interaction is cancelled rather than used. A vault can only eject loot — it
 * has no "unlocked" hook to hang the portal off — and letting it run would also mean its own
 * per-player unlock bookkeeping deciding who may open a portal, on top of ours. Cancelling and
 * consuming the totem here keeps one source of truth for both the cost and the ownership.
 */
@RequiredArgsConstructor
public class AntimatterPortalListener implements Listener {

    private final ForceItemBattle plugin;

    /**
     * Players currently being sent through, so the move handler does not fire again mid-teleport.
     */
    private final Set<UUID> travelling = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onVaultInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        AntimatterPortalManager portals = this.plugin.getAntimatterPortalManager();
        if (!portals.isPortalVault(block)) {
            return;
        }

        ItemStack held = event.getItem();
        if (!CustomMaterials.TOTEM_OF_ANTIMATTER.matches(held)) {
            // Stop the vault taking anything else, and say why — a vault with the wrong key in hand
            // gives no feedback at all otherwise.
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    Text.of("<dark_purple>This portal needs a <light_purple>Totem of Antimatter<dark_purple>."));
            return;
        }

        // From here the vault must not run: it would consume the totem on its own terms and eject
        // its loot table instead of opening anything.
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!this.plugin.getGamemanager().isMidGame() && !this.plugin.getGamemanager().isEndGame()) {
            return;
        }

        if (portals.activate(player, block)) {
            consumeOneFromMainHand(player);
        }
    }

    /**
     * Writes the reduced stack back explicitly rather than mutating the one the event handed us —
     * whether that stack is a live mirror of the inventory slot or a copy is an implementation
     * detail, and a totem silently surviving its own portal would be an easy exploit.
     */
    private void consumeOneFromMainHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        int left = hand.getAmount() - 1;
        if (left <= 0) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        hand.setAmount(left);
        player.getInventory().setItemInMainHand(hand);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.travelling.contains(player.getUniqueId())) {
            return;
        }

        AntimatterPortalManager portals = this.plugin.getAntimatterPortalManager();
        if (portals.isAntimatterWorld(player.getWorld())) {
            if (portals.isInReturnPortal(player)) {
                travel(player, this::sendHome);
            }
            return;
        }

        AntimatterPortalManager.ActivePortal portal = portals.portalPlayerIsStandingIn(player);
        if (portal != null) {
            travel(player, traveller -> sendToDepths(traveller, portal));
        }
    }

    /**
     * Runs a teleport with the player flagged, so the move events the teleport itself generates
     * cannot re-enter and bounce them straight back out again.
     */
    private void travel(Player player, java.util.function.Consumer<Player> destination) {
        this.travelling.add(player.getUniqueId());
        try {
            destination.accept(player);
        } finally {
            this.travelling.remove(player.getUniqueId());
        }
    }

    private void sendToDepths(Player player, AntimatterPortalManager.ActivePortal portal) {
        AntimatterPortalManager portals = this.plugin.getAntimatterPortalManager();
        Location depths = portals.depthsFor(player);
        if (depths == null) {
            player.sendMessage(Text.of("<red>The Antimatter Depths would not open. Tell an admin."));
            return;
        }
        portals.rememberReturn(player, portal);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1.2f);
        player.teleport(depths);
        player.playSound(depths, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.7f);
    }

    private void sendHome(Player player) {
        Location home = this.plugin.getAntimatterPortalManager().returnFor(player);
        if (home == null) {
            player.sendMessage(Text.of("<red>Nothing to return to — you did not arrive through a portal."));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1.2f);
        player.teleport(home);
    }

    /**
     * Vanilla still wants to run its own portal logic on the return portal's blocks. It has nowhere
     * sensible to send anyone from a custom dimension, and the walk-in handler above has already
     * dealt with it, so the attempt is stopped here before {@code PortalListener} sees it.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(PlayerPortalEvent event) {
        if (this.plugin.getAntimatterPortalManager().isAntimatterWorld(event.getFrom().getWorld())) {
            event.setCancelled(true);
        }
    }

    /**
     * A portal is hidden from everyone but its owner at the moment it is spawned, which cannot
     * cover players who were not online then.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.plugin.getAntimatterPortalManager().hideForeignPortals(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.travelling.remove(event.getPlayer().getUniqueId());
    }
}
