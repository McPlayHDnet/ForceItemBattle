package forceitembattle.model;

import forceitembattle.gui.ItemBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public enum CustomMaterials {

    ANTIMATTER_LOCATOR(Material.KNOWLEDGE_BOOK, "antimatter_locator", "Antimatter Locator", "<dark_purple>"),
    TRIAL_LOCATOR(Material.WITHER_ROSE, "trial_locator", "Trial Locator", "<gold>"),
    SULFUR_LOCATOR(Material.MUSIC_DISC_CHIRP, "sulfur_locator", "Sulfur Locator", "<yellow>"),
    WEATHERED_CAPTAINS_JOURNAL(Material.TORCHFLOWER, "journal_book", "Weathered Captain's Journal", "<green>");

    private static final Map<Material, CustomMaterials> BY_MATERIAL = Arrays.stream(values())
            .collect(Collectors.toMap(CustomMaterials::getMaterial, Function.identity()));
    private static final Map<String, CustomMaterials> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(CustomMaterials::getId, Function.identity()));

    private final Material material;
    private final String id;
    private final String itemName;
    private final String color;

    CustomMaterials(Material material, String id, String itemName, String color) {
        this.material = material;
        this.id = id;
        this.itemName = itemName;
        this.color = color;
    }

    /**
     * MiniMessage name put on the actual item, e.g. {@code » Antimatter Locator}.
     */
    public String displayName() {
        return "<dark_gray>» " + this.color + this.itemName;
    }

    public ItemStack itemStack() {
        return new ItemBuilder(this.material).setDisplayName(this.displayName()).getItemStack();
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
     * Coloured display name if the material is one of ours, {@code null} otherwise —
     * {@code ItemBuilder#setDisplayName(null)} is a no-op, so the vanilla name survives.
     */
    @Nullable
    public static String displayNameOf(Material material) {
        CustomMaterials custom = byMaterial(material);
        return custom != null ? custom.displayName() : null;
    }

    /**
     * The /info id if the material is one of ours, the lowercase material name otherwise.
     */
    public static String idOf(Material material) {
        CustomMaterials custom = byMaterial(material);
        return custom != null ? custom.getId() : material.name().toLowerCase();
    }
}
