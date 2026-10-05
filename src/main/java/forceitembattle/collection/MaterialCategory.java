package forceitembattle.collection;

import java.util.List;
import java.util.Set;
import org.bukkit.Material;

/** Categorises materials by type, independently of the ItemDifficultiesManager's state system. */
public class MaterialCategory {

    /** Longest first, load-bearing: DARK_OAK_LOG contains both DARK_OAK and OAK, and the first match wins. */
    private static final List<String> WOOD_TYPES = List.of(
            "DARK_OAK", "PALE_OAK", "MANGROVE",
            "CRIMSON",
            "SPRUCE", "JUNGLE", "ACACIA", "CHERRY", "WARPED", "POPLAR",
            "BIRCH",
            "OAK"
    );

    /** WOODEN_SWORD and friends are excluded because they don't say which wood they are made of. */
    public static boolean isWoodType(Material material) {
        return isWoodType(material.name());
    }

    public static boolean isWoodType(String name) {
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
        return getWoodCategory(material.name());
    }

    public static String getWoodCategory(String name) {
        if (!isWoodType(name)) {
            return null;
        }

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
        return isStoneType(material.name());
    }

    public static boolean isStoneType(String name) {
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
        return isTool(material.name());
    }

    public static boolean isTool(String name) {
        return name.endsWith("_SWORD") || name.endsWith("_AXE") ||
                name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE");
    }

    public static boolean isArmor(Material material) {
        return isArmor(material.name());
    }

    public static boolean isArmor(String name) {
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    /**
     * Mirrors {@code Material.isEdible()} without the registry. Fish buckets are absent on purpose (food but
     * not consumable); kept in sync by {@code MaterialCategoryTest}.
     */
    private static final Set<String> EDIBLE = Set.of(
            "APPLE", "BAKED_POTATO", "BEEF", "BEETROOT",
            "BEETROOT_SOUP", "BREAD", "CARROT", "CHICKEN",
            "CHORUS_FRUIT", "COD", "COOKED_BEEF", "COOKED_CHICKEN",
            "COOKED_COD", "COOKED_MUTTON", "COOKED_PORKCHOP", "COOKED_RABBIT",
            "COOKED_SALMON", "COOKIE", "DRIED_KELP", "ENCHANTED_GOLDEN_APPLE",
            "GLOW_BERRIES", "GOLDEN_APPLE", "GOLDEN_CARROT", "HONEY_BOTTLE",
            "MELON_SLICE", "MUSHROOM_STEW", "MUTTON", "POISONOUS_POTATO",
            "PORKCHOP", "POTATO", "PUFFERFISH", "PUMPKIN_PIE",
            "RABBIT", "RABBIT_STEW", "ROTTEN_FLESH", "SALMON",
            "SPIDER_EYE", "SUSPICIOUS_STEW", "SWEET_BERRIES", "TROPICAL_FISH"
    );

    public static boolean isFood(Material material) {
        return isFood(material.name());
    }

    public static boolean isFood(String materialName) {
        return EDIBLE.contains(materialName);
    }

    public static Set<String> getAllWoodCategories() {
        return Set.of("OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA",
                "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK", "POPLAR",
                "CRIMSON", "WARPED", "BAMBOO");
    }

}
