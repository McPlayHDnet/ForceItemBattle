package forceitembattle.listener;

import forceitembattle.fairplay.ModDetections;
import forceitembattle.fairplay.ModFinding;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class XaeroMinimapListener implements Listener {

    static final String MINIMAP_CHANNEL = "xaerominimap:main";

    // Xaero's reads these from the raw text of a system message, so they must stay literal § codes and
    // never pass through MiniMessage or the legacy serializer.
    static final String DISABLE_MINIMAP = "§n§o§m§i§n§i§m§a§p";
    static final String FAIR_PLAY = "§f§a§i§r§x§a§e§r§o";

    private final ModDetections modDetections;
    private final Set<UUID> disabled = new HashSet<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getListeningPluginChannels().contains(MINIMAP_CHANNEL)) {
            disable(player);
        }
    }

    // Fabric registers play-phase channels after the join, so the join check alone misses them.
    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equals(MINIMAP_CHANNEL) && event.getPlayer().isOnline()) {
            disable(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.disabled.remove(event.getPlayer().getUniqueId());
    }

    private void disable(Player player) {
        if (!this.disabled.add(player.getUniqueId())) {
            return;
        }

        player.sendMessage(Component.text(DISABLE_MINIMAP));
        player.sendMessage(Component.text(FAIR_PLAY));
        this.modDetections.record(player.getUniqueId(), player.getName(), ModFinding.XAERO_MINIMAP);
    }
}
