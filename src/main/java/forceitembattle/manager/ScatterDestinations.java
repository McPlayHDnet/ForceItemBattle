package forceitembattle.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;

/**
 * Where a scatter sends a player, and where it has sent them before. See {@code CONTEXT.md § Scatter}.
 *
 * <p>Owns the memory and the draw; {@code PortalListener} owns the world — grounding a destination,
 * laying a floor under it, and the teleport.
 *
 * <p>Nothing clears the memory, which is safe only because {@code scheduleReset} restarts the JVM
 * between rounds.
 */
public final class ScatterDestinations {

    /** Half the reuse radius, squared: a portal within 25 blocks counts as the same portal. */
    private static final double SAME_PORTAL_DISTANCE_SQUARED = 625;

    private static final int MINIMUM_OFFSET = 5_000;
    private static final int OFFSET_SPREAD = 10_001;

    private final Map<UUID, List<Scatter>> byTeleporter = new HashMap<>();
    private final Map<UUID, Location> byPlayerInTheEnd = new HashMap<>();
    private final Random random;

    public ScatterDestinations(Random random) {
        this.random = random;
    }

    public ScatterDestinations() {
        this(new Random());
    }

    /** Where a scatter from {@code origin} has already sent this player, if it has. */
    public Optional<Location> existingTeleporterDestination(UUID playerUuid, Location origin) {
        return this.byTeleporter.getOrDefault(playerUuid, List.of()).stream()
                .filter(scatter -> scatter.isSamePortalAs(origin))
                .map(Scatter::destination)
                .findFirst();
    }

    /** Records where {@code origin} sent this player, so a second trip repeats it. */
    public void rememberTeleporter(UUID playerUuid, Location origin, Location destination) {
        this.byTeleporter.computeIfAbsent(playerUuid, key -> new ArrayList<>())
                .add(new Scatter(origin, destination));
    }

    /** Per player, not per portal: there is one End, and returning to it should not re-scatter you. */
    public Optional<Location> existingEndDestination(UUID playerUuid) {
        return Optional.ofNullable(this.byPlayerInTheEnd.get(playerUuid));
    }

    public void rememberEnd(UUID playerUuid, Location destination) {
        this.byPlayerInTheEnd.put(playerUuid, destination);
    }

    /** {@code origin} moved 5000..15000 blocks per axis. The caller grounds the height. */
    public Location scatterTargetFrom(Location origin) {
        return new Location(origin.getWorld(),
                origin.getX() + axisOffset(),
                origin.getY(),
                origin.getZ() + axisOffset());
    }

    /** 5000..15000 blocks out on one axis, sign picked separately. */
    private int axisOffset() {
        int magnitude = this.random.nextInt(OFFSET_SPREAD) + MINIMUM_OFFSET;
        return this.random.nextBoolean() ? magnitude : -magnitude;
    }

    private record Scatter(Location portal, Location destination) {

        boolean isSamePortalAs(Location location) {
            return this.portal.getWorld() == location.getWorld()
                    && this.portal.distanceSquared(location) <= SAME_PORTAL_DISTANCE_SQUARED;
        }
    }
}
