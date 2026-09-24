package forceitembattle.listener;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import forceitembattle.moddetection.ModDetections;
import forceitembattle.moddetection.ModFinding;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class FreecamListener implements Listener {

    // The sign editor resolves translation keys client-side and sends the result back on close:
    // vanilla returns the raw key, a client with Freecam's lang file returns its translation.
    static final String PROBE_KEY = "key.freecam.toggle";

    // Freecam spawns its camera as a client-side entity with this id (tripods count down from it).
    static final int CAMERA_ENTITY_ID = -420;
    private static final int CAMERA_ENTITY_IDS = 10;

    private static final long CLOSE_DELAY_TICKS = 2;
    private static final long TIMEOUT_TICKS = 100;

    private final ModDetections modDetections;
    private final Map<UUID, Location> pendingProbes = new HashMap<>();
    private final Set<UUID> probed = new HashSet<>();

    @EventHandler
    public void onClientLoaded(PlayerClientLoadedWorldEvent event) {
        Player player = event.getPlayer();
        if (this.probed.add(player.getUniqueId())) {
            probe(player);
        }
    }

    private void probe(Player player) {
        Location location = player.getLocation().toBlockLocation();
        location.setY(player.getWorld().getMinHeight());

        BlockData signData = Material.OAK_SIGN.createBlockData();
        Sign sign = (Sign) signData.createBlockState();
        sign.getSide(Side.FRONT).line(0, Component.translatable(PROBE_KEY));

        this.pendingProbes.put(player.getUniqueId(), location);
        player.sendBlockChange(location, signData);
        player.sendBlockUpdate(location, sign);
        player.openVirtualSign(Position.block(location), Side.FRONT);

        Scheduler.runLaterSync(() -> {
            if (this.pendingProbes.get(player.getUniqueId()) == location && player.isOnline()
                    && player.getOpenInventory().getType() == InventoryType.CRAFTING) {
                player.closeInventory();
            }
        }, CLOSE_DELAY_TICKS);
        Scheduler.runLaterSync(() -> {
            if (this.pendingProbes.remove(player.getUniqueId(), location)) {
                restore(player, location);
            }
        }, TIMEOUT_TICKS);
    }

    @EventHandler
    public void onSignResponse(UncheckedSignChangeEvent event) {
        Player player = event.getPlayer();
        Location location = this.pendingProbes.get(player.getUniqueId());
        if (location == null || !isAt(event.getEditedBlockPosition(), location)) {
            return;
        }

        this.pendingProbes.remove(player.getUniqueId());
        event.setCancelled(true);
        restore(player, location);

        String response = PlainTextComponentSerializer.plainText().serialize(event.lines().getFirst());
        if (!response.isEmpty() && !response.equals(PROBE_KEY)) {
            this.modDetections.record(player.getUniqueId(), player.getName(), ModFinding.FREECAM_INSTALLED);
        }
    }

    @EventHandler
    public void onUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
        int id = event.getEntityId();
        Player player = event.getPlayer();
        boolean freecamCamera = id <= CAMERA_ENTITY_ID && id > CAMERA_ENTITY_ID - CAMERA_ENTITY_IDS;
        if (freecamCamera && this.modDetections.record(player.getUniqueId(), player.getName(), ModFinding.FREECAM_IN_USE)) {
            Bukkit.broadcast(Text.of(ModFinding.FREECAM_IN_USE.message(player.getName())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        this.pendingProbes.remove(uuid);
        this.probed.remove(uuid);
    }

    private static boolean isAt(BlockPosition position, Location location) {
        return position.blockX() == location.getBlockX()
                && position.blockY() == location.getBlockY()
                && position.blockZ() == location.getBlockZ();
    }

    private static void restore(Player player, Location location) {
        if (player.isOnline()) {
            player.sendBlockChange(location, location.getBlock().getBlockData());
        }
    }
}
