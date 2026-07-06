package forceitembattle.util;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public class CustomItem {

    private final Material material;
    private final int customModelData;
    private final String customModelDataString;
    private final String checkedName;

    // Persistent-data (custom_data under PublicBukkitValues) match: key is a
    // "namespace:key" string, value is the expected String value.
    private final String customDataKey;
    private final String customDataValue;

    // Full constructor
    public CustomItem(Material material, int customModelData, String customModelDataString, String checkedName,
                      String customDataKey, String customDataValue) {
        this.material = material;
        this.customModelData = customModelData;
        this.customModelDataString = customModelDataString;
        this.checkedName = checkedName;
        this.customDataKey = customDataKey;
        this.customDataValue = customDataValue;
    }

    // Full constructor without custom data
    public CustomItem(Material material, int customModelData, String customModelDataString, String checkedName) {
        this(material, customModelData, customModelDataString, checkedName, null, null);
    }

    // Legacy constructor (integer custom model data)
    public CustomItem(Material material, int customModelData, String checkedName) {
        this(material, customModelData, null, checkedName, null, null);
    }

    // String-based custom model data constructor
    public CustomItem(Material material, String customModelDataString, String checkedName) {
        this(material, 0, customModelDataString, checkedName, null, null);
    }

    // Name-only constructor
    public CustomItem(Material material, String checkedName) {
        this(material, 0, null, checkedName, null, null);
    }

    // Material-only constructor
    public CustomItem(Material material) {
        this(material, 0, null, null, null, null);
    }

    //  Matches purely on a persistent-data (custom_data / PublicBukkitValues) string tag
    public static CustomItem customData(String customDataKey, String customDataValue) {
        return new CustomItem(null, 0, null, null, customDataKey, customDataValue);
    }
}
