package forceitembattle.model;

import org.bukkit.Material;

/** {@code isAir()}, not {@code isSolid()} (false for water and snow layers) or {@code isBlock()} (true for AIR). */
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
