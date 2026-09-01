package forceitembattle.manager;

import forceitembattle.model.Roster;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.ProtectionVerdict;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * What may be broken, placed and opened during a round, and who was standing nearby when something
 * tried not to be. These are the rules; {@code ProtectionListener} is the adapter that cancels
 * events and says so.
 */
public class ProtectionManager implements Manager {

    /** 15 blocks, squared. Anyone inside this is named in the operator notification. */
    private static final double WITNESS_RADIUS_SQUARED = 225;
    private final Roster roster;
    private final Gamemanager gamemanager;
    private final Map<Block, UUID> containerMap;

    public ProtectionManager(Roster roster, Gamemanager gamemanager) {
        this.roster = roster;
        this.gamemanager = gamemanager;
        this.containerMap = new HashMap<>();
    }

    public boolean isNearProtectedBed(@Nullable Player player, Location atLocation) {
        for (var entry : this.roster.players().entrySet()) {
            if (player != null && entry.getKey().equals(player.getUniqueId())) {
                continue;
            }

            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || p.getRespawnLocation() == null || p.getRespawnLocation().getWorld() != atLocation.getWorld()) {
                continue;
            }

            // 3 block protection radius, squared.
            if (p.getRespawnLocation().distanceSquared(atLocation) < 9) {
                if (atLocation.getBlockY() >= p.getRespawnLocation().getBlockY()) {
                    return true;
                }
            }
        }

        return false;
    }

    public ForceItemPlayer getContainerOwner(Block block) {
        return this.roster.get(this.containerMap.get(block));
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

    /**
     * The score owner's membership and nothing else. Checking the TEAM setting and then reading
     * {@code currentTeam().getPlayers()} throws whenever the setting is on and the breaker has no
     * team — the mismatch between "team mode" and "on a team" that the score owner settles.
     */
    public boolean areTeammates(ForceItemPlayer breaker, ForceItemPlayer owner) {
        return breaker.squad().contains(owner);
    }

    /**
     * Whether this may be broken. Pass {@code null} for both actors when nothing is behind the break
     * — fire, lava, an explosion — which owns nothing and is refused by every rule.
     *
     * <p>Two actors, on purpose: the bed rule keys on the {@link Player} because it compares respawn
     * locations and must not protect you from your own bed, while the container rule keys on the
     * roster entry because ownership is a score-owner question. Both are not always known — someone
     * with no roster entry still has a respawn point.
     */
    public ProtectionVerdict mayBreak(@Nullable Player actor, @Nullable ForceItemPlayer breaker, Block block) {
        if (this.isNearProtectedBed(actor, block.getLocation())) {
            return ProtectionVerdict.NEAR_BED;
        }
        if (!this.canBreakContainer(breaker, block)) {
            return ProtectionVerdict.CONTAINER_OWNED;
        }
        return ProtectionVerdict.ALLOWED;
    }

    /**
     * The hopper rule is a container rule wearing a different hat: a hopper under someone else's
     * chest drains it, so placing one is refused exactly where breaking the chest above would be.
     */
    public ProtectionVerdict mayPlace(@Nullable Player actor, @Nullable ForceItemPlayer placer, Block block) {
        if (this.isNearProtectedBed(actor, block.getLocation())) {
            return ProtectionVerdict.NEAR_BED;
        }
        if (block.getType() == Material.HOPPER
                && !this.canBreakContainer(placer, block.getRelative(BlockFace.UP))) {
            return ProtectionVerdict.CONTAINER_OWNED;
        }
        return ProtectionVerdict.ALLOWED;
    }

    /** Whether a block is shielded from something with no player behind it. */
    public boolean isProtectedFromNature(Block block) {
        return this.mayBreak(null, null, block).denied();
    }

    /**
     * Everyone close enough to have caused what happened there, for the operator notification. The
     * radius is generous on purpose: it names suspects, it does not prove anything.
     */
    public List<Player> witnesses(Location location) {
        List<Player> nearby = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(location) < WITNESS_RADIUS_SQUARED) {
                nearby.add(player);
            }
        }
        return nearby;
    }

    /** The witnesses as a comma-separated list of names, or {@code nobody}. */
    public String witnessNames(Location location) {
        List<Player> nearby = this.witnesses(location);
        if (nearby.isEmpty()) {
            return "nobody";
        }
        return nearby.stream().map(Player::getName).collect(Collectors.joining(", "));
    }


    public void protectContainer(ForceItemPlayer forceItemPlayer, Block block) {
        this.containerMap.put(block, forceItemPlayer.player().getUniqueId());
    }

    public void breakContainer(Block block) {
        this.containerMap.remove(block);
    }

}
