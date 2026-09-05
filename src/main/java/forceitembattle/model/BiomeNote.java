package forceitembattle.model;

import forceitembattle.gui.ItemBuilder;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

@Getter
public enum BiomeNote {

    DESERT(NamespacedKey.minecraft("desert"), "Desert", "<gold>", List.of(
            "a vast desert of golden sand",
            "a sea of dunes where no water runs",
            "sun-bleached wastes of endless sand"
    )),
    BADLANDS(NamespacedKey.minecraft("badlands"), "Badlands", "<red>", List.of(
            "the red-clay badlands",
            "broken hills streaked with rust and clay",
            "a maze of crimson mesas"
    )),
    WARM_OCEAN(NamespacedKey.minecraft("warm_ocean"), "Warm Ocean", "<aqua>", List.of(
            "warm, turquoise waters",
            "a shallow sea the colour of jade",
            "bright reefs beneath a warm tide"
    )),
    PALE_GARDEN(NamespacedKey.minecraft("pale_garden"), "Pale Garden", "<gray>", List.of(
            "a pale, mist-shrouded grove",
            "a forest drained of all colour",
            "ghostly woods where the leaves hang grey"
    )),
    CHERRY_GROVE(NamespacedKey.minecraft("cherry_grove"), "Cherry Grove", "<light_purple>", List.of(
            "a grove of pink cherry blossoms",
            "hills awash in falling pink petals",
            "a blush-coloured wood in eternal bloom"
    ));

    /** PDC marker holding the note's enum name. */
    public static final NamespacedKey NOTE_KEY = new NamespacedKey("fib", "note");

    private static final Material MATERIAL = Material.PAPER;
    private static final List<String> MODEL = List.of("old_paper");
    private static final List<String> LORE = List.of(
            "<gray>A water-stained scrap.",
            "<dark_gray>Right-click to read."
    );

    private final NamespacedKey biomeKey;
    private final String biomeName;
    private final String color;
    private final List<String> flavors;

    BiomeNote(NamespacedKey biomeKey, String biomeName, String color, List<String> flavors) {
        this.biomeKey = biomeKey;
        this.biomeName = biomeName;
        this.color = color;
        this.flavors = flavors;
    }

    public ItemStack itemStack() {
        return new ItemBuilder(MATERIAL)
                .setItemName(this.color + "Faded Note - " + this.biomeName)
                .setCustomModelDataStrings(MODEL)
                .setLore(LORE)
                .setPersistentData(NOTE_KEY, PersistentDataType.STRING, this.name())
                .getItemStack();
    }

    public String randomFlavor() {
        return this.flavors.get(ThreadLocalRandom.current().nextInt(this.flavors.size()));
    }

    public static BiomeNote random() {
        BiomeNote[] values = values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    /**
     * The note this stack is, or null if it isn't one.
     */
    @Nullable
    public static BiomeNote fromItem(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != MATERIAL) {
            return null;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return null;
        }

        String name = itemMeta.getPersistentDataContainer().get(NOTE_KEY, PersistentDataType.STRING);
        if (name == null) {
            return null;
        }

        try {
            return valueOf(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
