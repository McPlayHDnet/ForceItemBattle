package forceitembattle.model;

import forceitembattle.gui.ItemBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    ANTIMATTER_LOCATOR(Material.KNOWLEDGE_BOOK, "antimatter_locator", "Antimatter Locator", "<dark_purple>", null, null, null, null),
    TRIAL_LOCATOR(Material.WITHER_ROSE, "trial_locator", "Trial Locator", "<gold>", null, null, null, null),
    SULFUR_LOCATOR(Material.MUSIC_DISC_CHIRP, "sulfur_locator", "Sulfur Locator", "<yellow>", null, null, null, null),
    EYE_OF_ANTIMATTER(Material.TORCHFLOWER_SEEDS, "eye_of_antimatter", "Eye of Antimatter", "<dark_purple>", null,
            new NamespacedKey("fib", "eye_of_antimatter"), "eye_of_antimatter", null),
    WEATHERED_CAPTAINS_JOURNAL(Material.TORCHFLOWER, "journal_book", "Weathered Captain's Journal", null,
            new NamespacedKey("fib", "items/weathered_captains_journal"),
            new NamespacedKey("fib", "journal"), null, null),
    WHEEL_OF_FORTUNE(Material.NETHER_STAR, "wheel_of_fortune", "Wheel of Fortune", null, null, null,
            "wheel", "<yellow><b>Wheel of Fortune");

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

    @Nullable
    private final String displayNameOverride;

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
     * Custom-model-data string carried by this item, used both when building it and when
     * matching it. Null when the material alone is enough.
     */
    @Nullable
    private final String customModelDataString;

    /**
     * Resolved from {@link #itemLootTable} on enable by CustomItemManager.
     * Never handed out directly — always cloned.
     */
    @Setter
    @Nullable
    private ItemStack prototype;

    CustomMaterials(Material material, String id, String itemName, @Nullable String color,
                    @Nullable NamespacedKey itemLootTable, @Nullable NamespacedKey markerKey,
                    @Nullable String customModelDataString, @Nullable String displayNameOverride) {
        this.material = material;
        this.id = id;
        this.itemName = itemName;
        this.color = color;
        this.itemLootTable = itemLootTable;
        this.markerKey = markerKey;
        this.customModelDataString = customModelDataString;
        this.displayNameOverride = displayNameOverride;
    }

    /**
     * MiniMessage name put on the ItemStack, e.g. {@code » Antimatter Locator}.
     * Null for items whose name comes from their loot table.
     */
    @Nullable
    public String displayName() {
        if (this.displayNameOverride != null) {
            return this.displayNameOverride;
        }
        return this.color != null ? "<dark_gray>» " + this.color + this.itemName : null;
    }

    /**
     * A fresh stack of this custom item.
     */
    public ItemStack itemStack() {
        if (this.prototype != null) {
            return this.prototype.clone();
        }

        ItemBuilder itemBuilder = new ItemBuilder(this.material).setDisplayName(this.displayName());
        if (this.customModelDataString != null) {
            itemBuilder.setCustomModelDataStrings(List.of(this.customModelDataString));
        }
        // Only for items we build ourselves — a loot table owns the marker on the items it defines,
        // and those return above from the prototype.
        if (this.markerKey != null) {
            itemBuilder.setPersistentData(this.markerKey, PersistentDataType.BYTE, (byte) 1);
        }
        return itemBuilder.getItemStack();
    }

    /**
     * A fresh stack of this custom item, {@code amount} of them.
     */
    public ItemStack itemStack(int amount) {
        ItemStack itemStack = this.itemStack();
        itemStack.setAmount(amount);
        return itemStack;
    }

    /**
     * Whether this stack is the real custom item. Some entries own their material outright and
     * need no further check; others share it with a vanilla item and are told apart by a PDC
     * marker or a custom-model-data string.
     */
    public boolean matches(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != this.material) {
            return false;
        }
        if (this.markerKey == null && this.customModelDataString == null) {
            return true;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }

        if (this.markerKey != null
                && !itemMeta.getPersistentDataContainer().has(this.markerKey, PersistentDataType.BYTE)) {
            return false;
        }

        if (this.customModelDataString != null) {
            return itemMeta.hasCustomModelDataComponent()
                    && itemMeta.getCustomModelDataComponent().getStrings().contains(this.customModelDataString);
        }

        return true;
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

    /**
     * The minecraft.wiki page slug for a material, e.g. {@code Heart_of_the_Sea}.
     *
     * The wiki title-cases every word except a handful of short joining words, and a joining word
     * never leads a title — hence the {@code index > 0}. Match those words whole: a substring
     * replace over the finished slug lowercases the A inside Axe, Apple, Armor, Andesite and
     * Amethyst, and catches the "With" inside Wither.
     *
     * <p>Lives next to {@link #nameOf(Material)} because it is the same question — what is this
     * material called — asked for a URL instead of a chat line.
     */
    public static String wikiSlugOf(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder slug = new StringBuilder(material.name().length());

        for (int index = 0; index < words.length; index++) {
            if (index > 0) {
                slug.append('_');
            }
            String word = words[index];
            boolean joining = index > 0 && WIKI_LOWERCASE_WORDS.contains(word);
            slug.append(joining ? word : WordUtils.capitalize(word));
        }

        return slug.toString();
    }

    private static final Set<String> WIKI_LOWERCASE_WORDS =
            Set.of("and", "with", "of", "on", "a", "the");
}
