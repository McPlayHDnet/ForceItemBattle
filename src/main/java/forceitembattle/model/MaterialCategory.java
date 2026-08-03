package forceitembattle.model;

import java.util.Set;
import org.bukkit.Material;

/**
 * Categorizes materials by type, independently of the ItemDifficultiesManager's state system.
 */
public class MaterialCategory {

    // Wood type categories (for "Wait Wood?" achievement)
    private static final Set<String> WOOD_TYPES = Set.of(
            "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA",
            "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK",
            "CRIMSON", "WARPED"
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
        if (name.startsWith("BAMBOO_") || name.equals("BAMBOO_BLOCK")) {
            return true;
        }

        return false;
    }

    /**
     * The wood type of a material (e.g. "OAK", "SPRUCE"), or null if it is not a wood type.
     */
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

        if (name.startsWith("BAMBOO_") || name.equals("BAMBOO_BLOCK")) {
            return "BAMBOO";
        }

        return null;
    }

    /**
     * Every stone type and its variants (polished, bricks, tiles, cracked, chiseled, ...).
     */
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

        if (name.contains("STONE") || name.contains("COBBLESTONE")) {
            return true;
        }

        if (name.contains("DEEPSLATE")) {
            return true;
        }

        if (name.contains("BLACKSTONE")) {
            return true;
        }

        if (name.contains("ANDESITE")) {
            return true;
        }

        if (name.contains("DIORITE")) {
            return true;
        }

        if (name.contains("GRANITE")) {
            return true;
        }

        if (name.contains("TUFF")) {
            return true;
        }

        return false;
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

    public static int getRequiredWoodCategoriesCount() {
        return getAllWoodCategories().size();
    }
}
