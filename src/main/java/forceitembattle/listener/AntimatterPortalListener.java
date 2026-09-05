package forceitembattle.listener;

import forceitembattle.model.RoundPhase;
import forceitembattle.manager.AntimatterPortalManager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.Dimension;
import forceitembattle.util.Text;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Turns a Totem of Antimatter placed in an Antimatter Depths portal vault into an open portal, and
 * walks its owner through when they step into it.
 *
 * <p>The vanilla vault interaction is cancelled rather than used: a vault has no "unlocked" hook to
 * hang the portal off, and letting it run would put its own per-player unlock bookkeeping in charge
 * of who may open a portal, on top of ours.
 */
@RequiredArgsConstructor
public class AntimatterPortalListener implements Listener {
    private final AntimatterPortalManager antimatterPortalManager;
    private final RoundPhase roundPhase;
    /** Players being sent through, so the move handler does not fire again mid-teleport. */
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

        AntimatterPortalManager portals = this.antimatterPortalManager;
        if (!portals.isPortalVault(block)) {
            return;
        }

        ItemStack held = event.getItem();
        if (!CustomMaterials.TOTEM_OF_ANTIMATTER.matches(held)) {
            // A vault with the wrong key in hand gives no feedback at all otherwise.
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    Text.of("<dark_purple>This portal needs a <light_purple>Totem of Antimatter<dark_purple>."));
            return;
        }

        // The vault must not run: it would consume the totem on its own terms and eject its loot.
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!this.roundPhase.roundRunning() && !this.roundPhase.isEndGame()) {
            return;
        }

        if (portals.activate(player, block)) {
            consumeOneFromMainHand(player);
        }
    }

    /**
     * Writes the reduced stack back explicitly rather than mutating the one the event handed us:
     * whether that is a live mirror of the slot or a copy is an implementation detail, and a totem
     * surviving its own portal is an easy exploit.
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

        AntimatterPortalManager portals = this.antimatterPortalManager;
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
     * Flags the player, so the move events the teleport itself generates cannot re-enter and bounce
     * them straight back out.
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
        AntimatterPortalManager portals = this.antimatterPortalManager;
        Location depths = portals.depthsFor(player);
        if (depths == null) {
            // Usually recoverable: the manager refuses a Depths it cannot bring the player home from,
            // and having claimed that one it hands out a different one next time.
            player.sendMessage(Text.of("<dark_purple>The Depths would not take you. "
                    + "<gray>Step through again — if it keeps failing, tell eltobito."));
            return;
        }
        portals.rememberReturn(player, portal);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1.2f);
        player.teleport(depths);
        player.playSound(depths, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.7f);
    }

    private void sendHome(Player player) {
        Location home = this.antimatterPortalManager.returnFor(player);
        if (home == null) {
            player.sendMessage(Text.of("<red>Nothing to return to — you did not arrive through a portal."));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1.2f);
        player.teleport(home);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(PlayerPortalEvent event) {
        if (!this.antimatterPortalManager.isAntimatterWorld(event.getFrom().getWorld())) {
            return;
        }
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            return;
        }

        World end = Dimension.END.world();
        if (end == null) {
            event.setCancelled(true);
            return;
        }
        if (event.getTo() == null || !end.equals(event.getTo().getWorld())) {
            event.setTo(end.getSpawnLocation());
        }
    }

    /** Portals are hidden at spawn time, which cannot cover players who were not online then. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.antimatterPortalManager.hideForeignPortals(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.travelling.remove(event.getPlayer().getUniqueId());
    }
}
