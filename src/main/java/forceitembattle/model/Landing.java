package forceitembattle.model;

import org.bukkit.Material;

/**
 * Whether a scatter destination has anything to stand on. Both scatter paths in
 * {@code PortalListener} drop a player onto the highest block in the column they land in; when the
 * column is empty there is no highest block and the destination is mid air.
 *
 * <p>{@link Material#isAir()} and not {@link Material#isSolid()}, deliberately: {@code isSolid()} is
 * false for water, lava, snow layers and tall grass, so it would plug an ocean landing with a stone
 * column and leave a scar on every snowy or grassy arrival. And not {@link Material#isBlock()},
 * which asks whether a material is a <em>placeable block type</em> — true for AIR, WATER and LAVA
 * alike, so a guard built on it never places the floor at all.
 */
public final class Landing {

    private Landing() {
    }

    /**
     * @param below the material at destination minus one block, as {@code World#getHighestBlockYAt}
     *              left it
     */
    public static boolean needsFloor(Material below) {
        return below.isAir();
    }
}
