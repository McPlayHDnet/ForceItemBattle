package forceitembattle.manager;

import forceitembattle.util.ParticleUtils;
import forceitembattle.util.Scheduler;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

    /** Redraw faster than dust fades, so the trail looks like it is lying there. */
    private static final long FOOTPRINT_REDRAW_TICKS = 10L;
    private static final int FOOTPRINT_COUNT = 12;
    private static final double FOOTPRINT_STRIDE = 1.4;
    /** Sideways offset from the walking line, alternating left and right. */
    private static final double FOOTPRINT_STRIDE_WIDTH = 0.3;
    private static final float FOOTPRINT_DUST_SIZE = 0.7f;
    /** How far above and below the player's feet a step looks for ground. */
    private static final int FOOTPRINT_GROUND_UP = 2;
    private static final int FOOTPRINT_GROUND_DOWN = 4;

    /** Offsets of one footprint's dust, as {along travel, across travel} in blocks. */
    private static final double[][] FOOTPRINT_SHAPE = {
            {0.18, 0.00}, {0.12, 0.07}, {0.12, -0.07}, {0.00, 0.09}, {0.00, -0.09},
            {-0.12, 0.06}, {-0.12, -0.06}, {-0.18, 0.00},
    };

    private final Map<String, Location> positionsMap;

    public PositionManager() {
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

        Location target = position.clone().add(0, 0.3, 0);

        Scheduler.runTimerSync(new BukkitRunnable() {
            private static final double SPACING = 0.5;
            private static final double FLOW_STEP = 0.25;
            /** Horizontal distance within which the line reveals the target's depth. */
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
             * Aims level while the target is still far below, so the line does not dip into the
             * ground long before the dig spot; it only angles down once close.
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
        }, 0L, 10L);
    }

    /** A short-lived (~5s) line of footprints across the ground towards the target. */
    public void playFootprintTrail(@NonNull Player player, @NonNull Location position, Color color) {
        if (player.getWorld() != position.getWorld()) return;
        Scheduler.runTimerSync(this.createFootprintTrail(player, position, color, 10), 0L, FOOTPRINT_REDRAW_TICKS);
    }

    /**
     * A footprint trail that keeps redrawing until the returned task is cancelled. Re-anchored on the
     * player each redraw, so it always leads away from where they are now.
     *
     * @return the trail task, or null when the player is in another world
     */
    @Nullable
    public BukkitRunnable startFootprintTrail(@NonNull Player player, @NonNull Location position, Color color) {
        if (player.getWorld() != position.getWorld()) return null;
        BukkitRunnable trail = this.createFootprintTrail(player, position, color, -1);
        Scheduler.runTimerSync(trail, 0L, FOOTPRINT_REDRAW_TICKS);
        return trail;
    }

    private BukkitRunnable createFootprintTrail(Player player, Location position, Color color, int maxRedraws) {
        return new BukkitRunnable() {
            int current = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if (++this.current == maxRedraws) {
                    this.cancel();
                }
                drawFootprintFrame(player, position, new Particle.DustOptions(color, FOOTPRINT_DUST_SIZE));
            }
        };
    }

    /**
     * One full trail of prints, from the player's feet towards the target. Each print snaps to
     * whatever a walker would step on nearby rather than to the world surface — underground the
     * surface is the roof of the cave, and the trail would be drawn in the ceiling.
     *
     * <p>Reads blocks, so main thread only: the async locator session schedules the task rather than
     * drawing itself.
     */
    private static void drawFootprintFrame(Player player, Location position, Particle.DustOptions dust) {
        Location from = player.getLocation();
        double dx = position.getX() - from.getX();
        double dz = position.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1) {
            return; // standing on it
        }

        double forwardX = dx / distance;
        double forwardZ = dz / distance;

        int prints = Math.min(FOOTPRINT_COUNT, (int) (distance / FOOTPRINT_STRIDE));
        for (int step = 1; step <= prints; step++) {
            // Left, right, left … a walk rather than a row of stamps.
            double side = (step % 2 == 0 ? FOOTPRINT_STRIDE_WIDTH : -FOOTPRINT_STRIDE_WIDTH);
            double x = from.getX() + forwardX * step * FOOTPRINT_STRIDE - forwardZ * side;
            double z = from.getZ() + forwardZ * step * FOOTPRINT_STRIDE + forwardX * side;

            Double y = groundAt(from, x, z);
            if (y != null) {
                drawFootprint(player, x, y, z, forwardX, forwardZ, dust);
            }
        }
    }

    /** Top face of the block a walker would step on around this spot, or null if there is none. */
    @Nullable
    private static Double groundAt(Location from, double x, double z) {
        for (int y = from.getBlockY() + FOOTPRINT_GROUND_UP; y >= from.getBlockY() - FOOTPRINT_GROUND_DOWN; y--) {
            Block block = from.getWorld().getBlockAt((int) Math.floor(x), y, (int) Math.floor(z));
            if (!block.isPassable() && block.getRelative(BlockFace.UP).isPassable()) {
                return block.getBoundingBox().getMaxY() + 0.05;
            }
        }
        return null;
    }

    private static void drawFootprint(Player player, double x, double y, double z,
                                      double forwardX, double forwardZ, Particle.DustOptions dust) {
        for (double[] offset : FOOTPRINT_SHAPE) {
            player.spawnParticle(Particle.DUST,
                    x + forwardX * offset[0] - forwardZ * offset[1],
                    y,
                    z + forwardZ * offset[0] + forwardX * offset[1],
                    1, dust);
        }
    }

    /**
     * A short-lived (~5s) beam with ground rings marking the spot to dig at. The location must be
     * resolved upfront — this only spawns particles.
     */
    public void playSurfaceMarker(@NonNull Player player, @NonNull Location surface, Color color) {
        if (player.getWorld() != surface.getWorld()) return;
        Scheduler.runTimerSync(this.createSurfaceMarker(player, surface, color, 10), 0L, 10L);
    }

    /**
     * A surface marker that keeps redrawing until the returned task is cancelled.
     *
     * @return the marker task, or null when the player is in another world
     */
    @Nullable
    public BukkitRunnable startSurfaceMarker(@NonNull Player player, @NonNull Location surface, Color color) {
        if (player.getWorld() != surface.getWorld()) return null;
        BukkitRunnable marker = this.createSurfaceMarker(player, surface, color, -1);
        Scheduler.runTimerSync(marker, 0L, 10L);
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
