package forceitembattle.collection;

import java.util.List;
import java.util.Set;
import org.bukkit.Material;

/** Categorises materials by type, independently of the ItemDifficultiesManager's state system. */
public class MaterialCategory {

    /**
     * Wood types, <b>longest first</b> — load-bearing. {@link #getWoodCategory} returns the first one
     * the material's name contains, and {@code DARK_OAK_LOG} contains both {@code DARK_OAK} and
     * {@code OAK}. A {@code Set} here randomises per JVM, so {@code DARK_OAK} stops being produced
     * at all on some runs and the wood-collection achievement becomes uncompletable.
     */
    private static final List<String> WOOD_TYPES = List.of(
            "DARK_OAK", "PALE_OAK", "MANGROVE",
            "CRIMSON",
            "SPRUCE", "JUNGLE", "ACACIA", "CHERRY", "WARPED",
            "BIRCH",
            "OAK"
    );

    /**
     * A specific wood type, not a generic "wooden" one: WOODEN_SWORD and friends are excluded
     * because they don't say which wood they are made of.
     */
    public static boolean isWoodType(Material material) {
        String name = material.name();

        if (name.startsWith("WOODEN_")) {
            return false;
        }

        if (name.equals("STICK") || name.equals("BOWL") || name.equals("LADDER") ||
                name.equals("CRAFTING_TABLE") || name.equals("CHEST") ||
                name.equals("TRAPPED_CHEST") || name.equals("BARREL")) {
            return false;
        }

        for (String woodType : WOOD_TYPES) {
            if (name.contains(woodType)) {
                return true;
            }
        }

        // Suffixed bamboo only -- BAMBOO itself is the plant.
        if (name.startsWith("BAMBOO_")) {
            return true;
        }

        return false;
    }

    /** The wood type of a material, e.g. "OAK", or null if it is not one. */
    public static String getWoodCategory(Material material) {
        if (!isWoodType(material)) {
            return null;
        }

        String name = material.name();

        for (String woodType : WOOD_TYPES) {
            if (name.contains(woodType)) {
                return woodType;
            }
        }

        if (name.startsWith("BAMBOO_")) {
            return "BAMBOO";
        }

        return null;
    }

    /** Every stone type and its variants (polished, bricks, tiles, cracked, chiselled, …). */
    public static boolean isStoneType(Material material) {
        String name = material.name();

        // Stone tools are tools, not stone blocks.
        if (name.startsWith("STONE_") && (name.endsWith("_SWORD") || name.endsWith("_AXE") ||
                name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE"))) {
            return false;
        }

        // Carry "STONE" in their name without being one.
        if (name.contains("REDSTONE") || name.equals("LODESTONE") ||
                name.equals("GRINDSTONE") || name.equals("DRIPSTONE")) {
            return false;
        }

        if (name.contains("END_STONE") || name.equals("ENDSTONE")) {
            return false;
        }

        if (name.contains("SANDSTONE")) {
            return false;
        }

        return name.contains("STONE") || name.contains("COBBLESTONE") || name.contains("DEEPSLATE")
                || name.contains("BLACKSTONE") || name.contains("ANDESITE") || name.contains("DIORITE")
                || name.contains("GRANITE") || name.contains("TUFF");
    }

    public static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") ||
                name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE");
    }

    public static boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    public static boolean isFood(Material material) {
        return material.isEdible();
    }

    public static Set<String> getAllWoodCategories() {
        return Set.of("OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA",
                "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK",
                "CRIMSON", "WARPED", "BAMBOO");
    }

}
