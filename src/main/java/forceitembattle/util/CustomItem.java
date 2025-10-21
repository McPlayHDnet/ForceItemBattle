package forceitembattle.util;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public class CustomItem {

    private final Material material;
    private final int customModelData;
    private final String customModelDataString;
    private final String checkedName;

    // Full constructor
    public CustomItem(Material material, int customModelData, String customModelDataString, String checkedName) {
        this.material = material;
        this.customModelData = customModelData;
        this.customModelDataString = customModelDataString;
        this.checkedName = checkedName;
    }

    // Legacy constructor (integer custom model data)
    public CustomItem(Material material, int customModelData, String checkedName) {
        this(material, customModelData, null, checkedName);
    }

    // String-based custom model data constructor
    public CustomItem(Material material, String customModelDataString, String checkedName) {
        this(material, 0, customModelDataString, checkedName);
    }

    // Name-only constructor
    public CustomItem(Material material, String checkedName) {
        this(material, 0, null, checkedName);
    }

    // Material-only constructor
    public CustomItem(Material material) {
        this(material, 0, null, null);
    }
}