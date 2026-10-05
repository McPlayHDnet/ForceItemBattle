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

    /** Trail ruins are visible from the surface, so that locator lets go sooner. */
    private final int arrivalRadius;

    private final Color lineColor;
    private final String bossBarGradient;

    /**
     * The structure set's {@code spacing} in chunks, copied because the server doesn't expose it; 0 for
     * a biome locator. Under the real value is safe, over it skips regions, so lower it if unsure.
     */
    private final int structureSpacing;

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

        /** Trail ruins lie at the surface, so there is nothing to dig down to. */
        public boolean leavesFootprints() {
            return this == BRUSH_GROUND;
        }
    }
}
