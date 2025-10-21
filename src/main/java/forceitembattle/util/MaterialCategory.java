package forceitembattle.util;

import org.bukkit.Material;

import java.util.Set;

/**
 * Utility class for categorizing materials by type.
 * This works independently of the ItemDifficultiesManager's state system.
 */
public class MaterialCategory {

    // Wood type categories (for "Wait Wood?" achievement)
    private static final Set<String> WOOD_TYPES = Set.of(
            "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA",
            "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK",
            "CRIMSON", "WARPED"
    );

    /**
     * Check if a material is a wood-related item (specific wood type, not generic "wooden")
     * This excludes generic wooden tools (WOODEN_SWORD, WOODEN_AXE, etc.) because they
     * don't specify which wood type they are.
     */
    public static boolean isWoodType(Material material) {
        String name = material.name();

        // Exclude generic "WOODEN_" items (tools that don't specify wood type)
        if (name.startsWith("WOODEN_")) {
            return false;
        }

        // Exclude other generic items that aren't specific wood types
        if (name.equals("STICK") || name.equals("BOWL") || name.equals("LADDER") ||
                name.equals("CRAFTING_TABLE") || name.equals("CHEST") ||
                name.equals("TRAPPED_CHEST") || name.equals("BARREL")) {
            return false;
        }

        // Check if material name contains any specific wood type
        for (String woodType : WOOD_TYPES) {
            if (name.contains(woodType)) {
                return true;
            }
        }

        // Special case: bamboo items (but not bamboo the plant itself without suffix)
        if (name.startsWith("BAMBOO_") || name.equals("BAMBOO_BLOCK")) {
            return true;
        }

        return false;
    }

    /**
     * Get the wood type category from a material (e.g., "OAK", "SPRUCE")
     * Returns null if not a wood type
     */
    public static String getWoodCategory(Material material) {
        if (!isWoodType(material)) {
            return null;
        }

        String name = material.name();

        // Check each wood type (these will catch most items)
        for (String woodType : WOOD_TYPES) {
            if (name.contains(woodType)) {
                return woodType;
            }
        }

        // Bamboo items (BAMBOO_PLANKS, BAMBOO_DOOR, etc.)
        if (name.startsWith("BAMBOO_") || name.equals("BAMBOO_BLOCK")) {
            return "BAMBOO";
        }

        return null;
    }

    /**
     * Check if a material is a stone-related item
     * Includes all stone types and their variants (polished, bricks, tiles, cracked, chiseled, etc.)
     */
    public static boolean isStoneType(Material material) {
        String name = material.name();

        // Exclude stone tools and weapons - they're tools, not stone blocks
        if (name.startsWith("STONE_") && (name.endsWith("_SWORD") || name.endsWith("_AXE") ||
                name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE"))) {
            return false;
        }

        // Exclude redstone-related items that contain "STONE" in name
        if (name.contains("REDSTONE") || name.equals("LODESTONE") ||
                name.equals("GRINDSTONE") || name.equals("DRIPSTONE")) {
            return false;
        }

        // Exclude end stone
        if (name.contains("END_STONE") || name.equals("ENDSTONE")) {
            return false;
        }

        // Exclude sandstone
        if (name.contains("SANDSTONE")) {
            return false;
        }

        // === STONE and variants ===
        // Includes: stone, cobblestone, mossy_cobblestone, stone_bricks, smooth_stone,
        // chiseled_stone_bricks, cracked_stone_bricks, mossy_stone_bricks, infested variants
        if (name.contains("STONE") || name.contains("COBBLESTONE")) {
            return true;
        }

        // === DEEPSLATE and variants ===
        // Includes: deepslate, cobbled_deepslate, polished_deepslate, deepslate_bricks,
        // deepslate_tiles, chiseled_deepslate, cracked_deepslate_bricks, cracked_deepslate_tiles,
        // reinforced_deepslate
        if (name.contains("DEEPSLATE")) {
            return true;
        }

        // === BLACKSTONE and variants ===
        // Includes: blackstone, polished_blackstone, polished_blackstone_bricks,
        // chiseled_polished_blackstone, cracked_polished_blackstone_bricks, gilded_blackstone
        if (name.contains("BLACKSTONE")) {
            return true;
        }

        // === ANDESITE and variants ===
        // Includes: andesite, polished_andesite
        if (name.contains("ANDESITE")) {
            return true;
        }

        // === DIORITE and variants ===
        // Includes: diorite, polished_diorite
        if (name.contains("DIORITE")) {
            return true;
        }

        // === GRANITE and variants ===
        // Includes: granite, polished_granite
        if (name.contains("GRANITE")) {
            return true;
        }

        // === TUFF and variants ===
        // Includes: tuff, polished_tuff, tuff_bricks, chiseled_tuff, chiseled_tuff_bricks
        if (name.contains("TUFF")) {
            return true;
        }

        return false;
    }

    /**
     * Check if a material is a tool
     */
    public static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") ||
                name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE");
    }

    /**
     * Check if a material is armor
     */
    public static boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    /**
     * Check if a material is food
     */
    public static boolean isFood(Material material) {
        return material.isEdible();
    }

    /**
     * Get all unique wood categories (for achievement tracking)
     */
    public static Set<String> getAllWoodCategories() {
        return Set.of("OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA",
                "DARK_OAK", "MANGROVE", "CHERRY", "PALE_OAK",
                "CRIMSON", "WARPED", "BAMBOO");
    }

    /**
     * Get the number of unique wood categories needed for "Wait Wood?" achievement
     */
    public static int getRequiredWoodCategoriesCount() {
        return getAllWoodCategories().size();
    }
}