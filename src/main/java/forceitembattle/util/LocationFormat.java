package forceitembattle.util;

import forceitembattle.model.Dimension;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public final class LocationFormat {

    private LocationFormat() {
    }

    public static String xyz(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return "<red>unknown location";
        }
        return "<dark_aqua>" + location.getBlockX()
                + "<gray>, <dark_aqua>" + location.getBlockY()
                + "<gray>, <dark_aqua>" + location.getBlockZ();
    }

    /**
     * For searches that resolve a column: their Y is not the surface and would mislead, so don't unify
     * with {@link #xyz}. The sulfur locator uses xyz because its Y comes from scanned blocks.
     */
    public static String xz(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return "<red>unknown location";
        }
        return "<dark_aqua>" + location.getBlockX()
                + "<gray>, <dark_aqua>?"
                + "<gray>, <dark_aqua>" + location.getBlockZ();
    }

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
