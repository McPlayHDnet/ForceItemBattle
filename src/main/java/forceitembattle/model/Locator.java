package forceitembattle.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Material;
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
     * How close counts as arrived, in blocks: the session ends there and the boss bar and trail go
     * away. Buried finds want you almost on top of them; trail ruins are visible from the surface,
     * so that locator lets go while you can still see where it was pointing.
     */
    private final int arrivalRadius;

    private final Color lineColor;
    private final String bossBarGradient;

    public Material getLocatorMaterial() {
        return this.locatorItem.getMaterial();
    }

    /**
     * Whether this stack is this locator's item. Locators whose material is theirs alone accept any
     * stack of it; the Kiln-Fired Brush has to be told apart from the plain brush players craft.
     */
    public boolean matches(ItemStack itemStack) {
        return this.locatorItem.matches(itemStack);
    }

    public enum Type {
        STRUCTURE,
        BIOME
    }

    /**
     * How the item is used — and, because each style is its own kind of tool, what using it costs
     * and what it leaves behind.
     */
    public enum Use {
        /** Right-click, anywhere. A one-shot charm: spent on a find, pointing the way with a particle line. */
        RIGHT_CLICK,
        /**
         * Right-click a block — with a brush in hand that reads as sweeping the ground. A tool, not
         * a charm: it survives every sweep and dusts a line of footprints towards the find.
         */
        BRUSH_GROUND;

        public boolean consumedOnFind() {
            return this == RIGHT_CLICK;
        }

        /**
         * Footprints across the ground, instead of a line through the air and a beam over the dig
         * spot. The two go together: what the brush finds is trail ruins, which lie at the surface,
         * so there is nothing to dig down to and nothing to put a beam over.
         */
        public boolean leavesFootprints() {
            return this == BRUSH_GROUND;
        }
    }
}
