package forceitembattle.util;

import forceitembattle.model.Dimension;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public final class LocationFormat {

    private LocationFormat() {
    }

    /**
     * {@code x, y, z} — for locations we actually know all three coordinates of
     * (saved positions, the wandering trader).
     */
    public static String xyz(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return "<red>unknown location";
        }
        return "<dark_aqua>" + location.getBlockX()
                + "<gray>, <dark_aqua>" + location.getBlockY()
                + "<gray>, <dark_aqua>" + location.getBlockZ();
    }

    /**
     * {@code x, ?, z} — for structure and biome searches, which resolve a column
     * rather than a point; the Y a {@code StructureSearchResult} carries is not
     * the surface Y and would mislead.
     * <p>
     * The hidden Y is deliberate. Do not "unify" this with {@link #xyz}.
     * <p>
     * One case has since earned the Y back, and it is the exception that keeps the rule: the
     * sulfur locator now generates the chunks and reads the blocks, so when {@code CaveScan}
     * returns a target its Y is a cavity someone looked at rather than a search artefact. That
     * path calls {@link #xyz} on purpose. Everything that has not been down there still calls
     * this.
     */
    public static String xz(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return "<red>unknown location";
        }
        return "<dark_aqua>" + location.getBlockX()
                + "<gray>, <dark_aqua>?"
                + "<gray>, <dark_aqua>" + location.getBlockZ();
    }

    /**
     * {@code (N blocks away)}, or {@code in the <nether>} when the target sits in
     * another dimension.
     */
    public static String distance(Location from, @Nullable Location to) {
        if (to == null || from.getWorld() == null || to.getWorld() == null) {
            return " <red>(unknown)";
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return " <gray>in the " + Dimension.of(to.getWorld()).coloredName();
        }
        return " <green>(" + (int) from.distance(to) + " blocks away)";
    }
}
