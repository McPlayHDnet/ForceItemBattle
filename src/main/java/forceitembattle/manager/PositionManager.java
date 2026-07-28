package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ParticleUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PositionManager implements Manager {

    private static final double MARKER_SPACING = 0.4;
    private static final double MARKER_RISE_STEP = 0.2;
    private static final double MARKER_BEAM_HEIGHT = 25;
    private static final float MARKER_DUST_SIZE = 1.75f;
    private static final double MARKER_RING_RADIUS = 1.5;
    private static final int MARKER_RING_POINTS = 16;
    private static final double MARKER_INNER_RING_RADIUS = 0.75;
    private static final int MARKER_INNER_RING_POINTS = 10;

    private final ForceItemBattle plugin;
    private final Map<String, Location> positionsMap;

    public PositionManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.positionsMap = new HashMap<>();
    }

    public boolean positionExist(String positionName) {
        return this.positionsMap.containsKey(positionName.toLowerCase());
    }

    public void createPosition(String positionName, Location location) {
        this.positionsMap.put(positionName.toLowerCase(), location);
    }

    public void removePosition(String positionName) {
        this.positionsMap.remove(positionName.toLowerCase());
    }

    public Map<String, Location> getAllPositions() {
        return this.positionsMap;
    }

    public void clearPositions() {
        this.positionsMap.clear();
    }

    public Location getPosition(String positionName) {
        return this.positionsMap.get(positionName.toLowerCase());
    }

    public void playParticleLine(@NonNull Player player, @NonNull Location position, Color color) {
        if (player.getWorld() != position.getWorld()) return;

        // Defining target location to
        Location target = position.clone().add(0, 0.3, 0);

        new BukkitRunnable() {
            private static final double SPACING = 0.5;
            private static final double FLOW_STEP = 0.25;
            /** Horizontal distance within which the line reveals the target's depth; equals the line's max reach. */
            private static final double DEPTH_REVEAL_DISTANCE = 50 * SPACING;
            int current = 0;

            @Override
            public void run() {
                if (++current == 10) {
                    this.cancel();
                }
                Location from = player.getLocation().add(0, 1.2, 0);
                double phase = (current * FLOW_STEP) % SPACING;
                ParticleUtils.drawLine(player, from, this.aim(from), Particle.DUST, new Particle.DustOptions(color, 1), 1, SPACING, 50, phase);
            }

            /**
             * Guides horizontally towards the target's x/z while it is still far below, so the line
             * doesn't dip into the ground long before the dig spot is reached. Only once close,
             * the line angles down to signal where to dig.
             */
            private Location aim(Location from) {
                double dx = target.getX() - from.getX();
                double dz = target.getZ() - from.getZ();
                boolean depthFarAway = target.getY() < from.getY()
                        && dx * dx + dz * dz > DEPTH_REVEAL_DISTANCE * DEPTH_REVEAL_DISTANCE;
                if (!depthFarAway) {
                    return target;
                }
                Location levelAim = target.clone();
                levelAim.setY(from.getY());
                return levelAim;
            }
        }.runTaskTimer(this.plugin, 0L, 10L);
    }

    /**
     * Draws a short-lived (~5s) surface marker: a tall rising particle beam with ground rings,
     * marking the exact spot to dig at. The location must be resolved upfront
     * (e.g. highest surface block), as this only spawns particles.
     */
    public void playSurfaceMarker(@NonNull Player player, @NonNull Location surface, Color color) {
        if (player.getWorld() != surface.getWorld()) return;
        this.createSurfaceMarker(player, surface, color, 10).runTaskTimer(this.plugin, 0L, 10L);
    }

    /**
     * Starts a persistent surface marker (see {@link #playSurfaceMarker}) that keeps redrawing
     * until the returned task is cancelled by the caller.
     *
     * @return the marker task, or null when the player is in another world
     */
    @Nullable
    public BukkitRunnable startSurfaceMarker(@NonNull Player player, @NonNull Location surface, Color color) {
        if (player.getWorld() != surface.getWorld()) return null;
        BukkitRunnable marker = this.createSurfaceMarker(player, surface, color, -1);
        marker.runTaskTimer(this.plugin, 0L, 10L);
        return marker;
    }

    private BukkitRunnable createSurfaceMarker(Player player, Location surface, Color color, int maxRedraws) {
        return new BukkitRunnable() {
            int current = 0;

            @Override
            public void run() {
                if (++this.current == maxRedraws) {
                    this.cancel();
                }
                Particle.DustOptions dust = new Particle.DustOptions(color, MARKER_DUST_SIZE);
                double phase = (this.current * MARKER_RISE_STEP) % MARKER_SPACING;
                drawSurfaceMarkerFrame(player, surface, dust, phase);
            }
        };
    }

    private static void drawSurfaceMarkerFrame(Player player, Location surface, Particle.DustOptions dust, double phase) {
        for (double y = phase; y < MARKER_BEAM_HEIGHT; y += MARKER_SPACING) {
            player.spawnParticle(Particle.DUST, surface.getX(), surface.getY() + y, surface.getZ(), 1, dust);
        }
        drawRing(player, surface, MARKER_RING_RADIUS, MARKER_RING_POINTS, dust);
        drawRing(player, surface, MARKER_INNER_RING_RADIUS, MARKER_INNER_RING_POINTS, dust);
    }

    private static void drawRing(Player player, Location center, double radius, int points, Particle.DustOptions dust) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            player.spawnParticle(Particle.DUST,
                    center.getX() + radius * Math.sin(angle),
                    center.getY(),
                    center.getZ() + radius * Math.cos(angle),
                    1, dust);
        }
    }

}
