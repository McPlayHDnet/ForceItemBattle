package forceitembattle.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class Locator {

    private final String structureId;
    private final String structureName;
    private final CustomMaterials locatorItem;
    private final Type type;

    public Material getLocatorMaterial() {
        return this.locatorItem.getMaterial();
    }

    public enum Type {
        STRUCTURE,
        BIOME
    }
}
