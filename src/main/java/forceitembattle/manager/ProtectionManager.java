package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionManager {

    private final ForceItemBattle plugin;

    private final Map<Block, UUID> containerMap;

    public ProtectionManager(ForceItemBattle plugin) {
        this.plugin = plugin;

        this.containerMap = new HashMap<>();
    }

    public boolean isNearProtectedBed(@Nullable Player player, Location atLocation) {
        for (var entry : this.plugin.getGamemanager().forceItemPlayerMap().entrySet()) {
            if (player != null && entry.getKey().equals(player.getUniqueId())) {
                // ignore breaking your own bed
                continue;
            }

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || p.getRespawnLocation() == null) {
                continue;
            }

            // 4 blocks protection radius
            if (p.getRespawnLocation().distanceSquared(atLocation) < 16) {
                return true;
            }
        }

        return false;
    }

    public ForceItemPlayer getContainerOwner(Block block) {
        return this.plugin.getGamemanager().getForceItemPlayer(this.containerMap.get(block));
    }

    public boolean canBreakContainer(@Nullable ForceItemPlayer player, Block block) {
        ForceItemPlayer owner = this.getContainerOwner(block);
        if (owner == null) {
            return true;
        }

        // Break is from a natural cause, e.g. fire/explosion. Disallow it.
        if (player == null) {
            return false;
        }

        return this.areTeammates(player, owner);
    }

    public boolean areTeammates(ForceItemPlayer breaker, ForceItemPlayer owner) {
        if (breaker.equals(owner)) {
            return true;
        }

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return breaker.currentTeam().getPlayers().contains(owner);
        }

        return false;
    }

    public void protectContainer(ForceItemPlayer forceItemPlayer, Block block) {
        this.containerMap.put(block, forceItemPlayer.player().getUniqueId());
    }

    public void breakContainer(Block block) {
        this.containerMap.remove(block);
    }

}
