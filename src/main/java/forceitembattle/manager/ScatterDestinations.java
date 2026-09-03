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
 * Where a scatter sends a player, and where it has sent them before.
 *
 * <p>Two scatters share this: the antimatter teleporter's, which remembers a destination per portal,
 * and the End's, which remembers one per player. Both draw from the same range and both must be
 * stable — walking back into a teleporter you have already used has to land you where it landed you
 * the first time, or the pad becomes a free reroll.
 *
 * <p>It owns the memory and the draw; the listener owns the world. Grounding a destination on the
 * highest block, laying a floor under it and the teleport itself all need a server and stay outside.
 * That split is what {@code AntimatterPortalListener} already does with
 * {@code AntimatterPortalManager}, and what this one did not.
 *
 * <p><b>The ordering hazard is gone rather than commented.</b> The reuse lookup used to be a
 * {@code computeIfAbsent} whose only job was to create the list that the <em>write</em> would then
 * assume existed — an ordering rule nothing enforced. Reading no longer writes.
 *
 * <p>Nothing clears the memory, which is safe only because {@code scheduleReset} restarts the JVM
 * between rounds. It is now a one-line change here rather than a change to a listener's fields.
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

    /** Records where the scatter at {@code origin} sent this player, so a second trip repeats it. */
    public void rememberTeleporter(UUID playerUuid, Location origin, Location destination) {
        this.byTeleporter.computeIfAbsent(playerUuid, key -> new ArrayList<>())
                .add(new Scatter(origin, destination));
    }

    /**
     * The End is remembered per player rather than per portal: there is one End, and a player who
     * comes back to it should return to the same island rather than being scattered again.
     */
    public Optional<Location> existingEndDestination(UUID playerUuid) {
        return Optional.ofNullable(this.byPlayerInTheEnd.get(playerUuid));
    }

    public void rememberEnd(UUID playerUuid, Location destination) {
        this.byPlayerInTheEnd.put(playerUuid, destination);
    }

    /**
     * {@code origin} moved 5000..15000 blocks on each axis, sign drawn per axis. The height is the
     * origin's: the caller grounds it on the world's highest block, which is the part that needs a
     * server.
     */
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
