package forceitembattle.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;

@Getter
@AllArgsConstructor
public class Locator {

    private final String structureId;
    private final String structureName;
    private final CustomMaterials locatorItem;
    private final Type type;
    private final Use use;

    /**
     * How close counts as arrived, in blocks. Buried finds want you almost on top of them; trail
     * ruins are visible from the surface, so that locator lets go sooner.
     */
    private final int arrivalRadius;

    private final Color lineColor;
    private final String bossBarGradient;

    public boolean matches(ItemStack itemStack) {
        return this.locatorItem.matches(itemStack);
    }

    public enum Type {
        STRUCTURE,
        BIOME
    }

    /** How the item is used, and with it what using it costs and what it leaves behind. */
    public enum Use {
        /** A one-shot charm: spent on a find, pointing the way with a particle line. */
        RIGHT_CLICK,
        /** A tool: survives every sweep and dusts a line of footprints towards the find. */
        BRUSH_GROUND;

        public boolean consumedOnFind() {
            return this == RIGHT_CLICK;
        }

        /**
         * Footprints instead of an air line and a beam: what the brush finds is trail ruins, which
         * lie at the surface, so there is nothing to dig down to.
         */
        public boolean leavesFootprints() {
            return this == BRUSH_GROUND;
        }
    }
}
