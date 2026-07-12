package forceitembattle.model;

import forceitembattle.gui.ItemBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

@Getter
public enum CustomMaterials {

    ANTIMATTER_LOCATOR(Material.KNOWLEDGE_BOOK, "antimatter_locator", "Antimatter Locator", "<dark_purple>", null, null),
    TRIAL_LOCATOR(Material.WITHER_ROSE, "trial_locator", "Trial Locator", "<gold>", null, null),
    SULFUR_LOCATOR(Material.MUSIC_DISC_CHIRP, "sulfur_locator", "Sulfur Locator", "<yellow>", null, null),
    WEATHERED_CAPTAINS_JOURNAL(Material.TORCHFLOWER, "journal_book", "Weathered Captain's Journal", null,
            new NamespacedKey("fib", "items/weathered_captains_journal"),
            new NamespacedKey("fib", "journal"));

    private static final Map<Material, CustomMaterials> BY_MATERIAL = Arrays.stream(values())
            .collect(Collectors.toMap(CustomMaterials::getMaterial, Function.identity()));
    private static final Map<String, CustomMaterials> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CustomMaterials::getId, Function.identity()));

    private final Material material;
    private final String id;
    private final String itemName;

    /**
     * MiniMessage colour used by {@link #displayName()}. Null when a loot table owns the name.
     */
    @Nullable
    private final String color;

    /**
     * Datapack loot table defining this item, or null when a plain rename is enough.
     */
    @Nullable
    private final NamespacedKey itemLootTable;

    /**
     * PDC marker distinguishing the real item from a plain stack of the same material,
     * or null when the material alone is enough.
     */
    @Nullable
    private final NamespacedKey markerKey;

    /**
     * Resolved from {@link #itemLootTable} on enable by CustomItemManager.
     * Never handed out directly — always cloned.
     */
    @Setter
    @Nullable
    private ItemStack prototype;

    CustomMaterials(Material material, String id, String itemName, @Nullable String color,
                    @Nullable NamespacedKey itemLootTable, @Nullable NamespacedKey markerKey) {
        this.material = material;
        this.id = id;
        this.itemName = itemName;
        this.color = color;
        this.itemLootTable = itemLootTable;
        this.markerKey = markerKey;
    }

    /**
     * MiniMessage name put on the ItemStack, e.g. {@code » Antimatter Locator}.
     * Null for items whose name comes from their loot table.
     */
    @Nullable
    public String displayName() {
        return this.color != null ? "<dark_gray>» " + this.color + this.itemName : null;
    }

    /**
     * A fresh stack of this custom item.
     */
    public ItemStack itemStack() {
        if (this.prototype != null) {
            return this.prototype.clone();
        }
        return new ItemBuilder(this.material).setDisplayName(this.displayName()).getItemStack();
    }

    /**
     * Whether this stack is the real custom item. See the class doc on why this is
     * material-only for some entries and marker-gated for others.
     */
    public boolean matches(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != this.material) {
            return false;
        }
        if (this.markerKey == null) {
            return true;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        return itemMeta != null && itemMeta.getPersistentDataContainer().has(this.markerKey, PersistentDataType.BYTE);
    }

    /**
     * The custom item for this material if there is one, a plain stack otherwise.
     * The single entry point for handing a force item to a player.
     */
    public static ItemStack itemStackOf(Material material) {
        CustomMaterials custom = byMaterial(material);
        return custom != null ? custom.itemStack() : new ItemStack(material);
    }

    @Nullable
    public static CustomMaterials byMaterial(Material material) {
        return BY_MATERIAL.get(material);
    }

    @Nullable
    public static CustomMaterials byId(String id) {
        return BY_ID.get(id.toLowerCase());
    }

    /**
     * Custom name if the material is one of ours, plain vanilla name otherwise.
     */
    public static String nameOf(Material material) {
        CustomMaterials custom = byMaterial(material);
        return custom != null
                ? custom.getItemName()
                : WordUtils.capitalizeFully(material.name().replace("_", " "));
    }

    /**
     * The /info id if the material is one of ours, the lowercase material name otherwise.
     */
    public static String idOf(Material material) {
        CustomMaterials custom = byMaterial(material);
        return custom != null ? custom.getId() : material.name().toLowerCase();
    }
}
