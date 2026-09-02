package forceitembattle.achievements;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import java.util.List;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/**
 * A match spec for items this plugin does <i>not</i> create — datapack loot-table
 * items such as Cavendish, Gros Michel and the legendary template. Items the plugin
 * builds itself live in {@link forceitembattle.model.CustomMaterials} instead.
 */

@Getter
public class CustomItemSpec {

    @Nullable
    private final Material material;

    /** Entry expected in the item's {@code custom_model_data} string list. */
    @Nullable
    private final String customModelDataString;

    /** PDC key, as {@code "namespace:key"}. */
    @Nullable
    private final String customDataKey;

    @Nullable
    private final String customDataValue;

    private CustomItemSpec(@Nullable Material material, @Nullable String customModelDataString,
                       @Nullable String customDataKey, @Nullable String customDataValue) {
        this.material = material;
        this.customModelDataString = customModelDataString;
        this.customDataKey = customDataKey;
        this.customDataValue = customDataValue;
    }

    /** Matches a material carrying the given custom-model-data string. */
    public static CustomItemSpec ofModelData(Material material, String customModelDataString) {
        return new CustomItemSpec(material, customModelDataString, null, null);
    }

    /** Matches purely on a persistent-data (custom_data / PublicBukkitValues) string tag. */
    public static CustomItemSpec customData(String customDataKey, String customDataValue) {
        return new CustomItemSpec(null, null, customDataKey, customDataValue);
    }

    /** Every criterion of this spec must hold; unset ones are simply not checked. */
    public boolean matches(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (this.material != null && item.getType() != this.material) {
            return false;
        }

        if (this.customModelDataString != null) {
            CustomModelData customModelData = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (customModelData == null) {
                return false;
            }
            List<String> strings = customModelData.strings();
            if (strings == null || !strings.contains(this.customModelDataString)) {
                return false;
            }
        }

        if (this.customDataKey != null) {
            NamespacedKey key = NamespacedKey.fromString(this.customDataKey);
            if (key == null) {
                return false;
            }
            String value = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            return value != null && value.equals(this.customDataValue);
        }

        return true;
    }
}
