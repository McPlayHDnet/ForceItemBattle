package forceitembattle.manager.customrecipe;

import forceitembattle.model.CustomMaterials;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public enum FakeRecipe {

    // Mirrors the real recipe in RecipeManager — one recipe, HARDER_TRACKERS does not change it.
    END_STRUCTURE(Material.KNOWLEDGE_BOOK, item ->
            new ShapedRecipe(key("antimatter_locator"), CustomMaterials.ANTIMATTER_LOCATOR.itemStack())
                    .shape(" B ", "GQG", " B ")
                    .setIngredient('B', Material.NETHER_BRICK)
                    .setIngredient('G', Material.GLOWSTONE_DUST)
                    .setIngredient('Q', Material.QUARTZ)
    ),

    CHAMBER_STRUCTURE(Material.WITHER_ROSE, item ->
            new ShapedRecipe(key("chambers_locator"), CustomMaterials.TRIAL_LOCATOR.itemStack())
                    .shape("BGB", "GCG", "AAA")
                    .setIngredient('B', Material.CUT_COPPER)
                    .setIngredient('G', Material.GLASS)
                    .setIngredient('C', Material.COMPASS)
                    .setIngredient('A', Material.GOLD_INGOT)
    ),

    CHAMBER_STRUCTURE_HARD(Material.WITHER_ROSE, item ->
            new ShapedRecipe(key("chambers_locator"), CustomMaterials.TRIAL_LOCATOR.itemStack())
                    .shape("OKO", "GCI", "ODO")
                    .setIngredient('O', Material.OBSIDIAN)
                    .setIngredient('C', Material.COMPASS)
                    .setIngredient('K', Material.COPPER_INGOT)
                    .setIngredient('I', Material.IRON_INGOT)
                    .setIngredient('G', Material.GOLD_INGOT)
                    .setIngredient('D', Material.DIAMOND)
    ),

    SUSPICIOUS_STEW(Material.SUSPICIOUS_STEW, item ->
            new ShapelessRecipe(key("suspicious_stew"), new ItemStack(Material.SUSPICIOUS_STEW))
                    .addIngredient(Material.BOWL)
                    .addIngredient(Material.RED_MUSHROOM)
                    .addIngredient(Material.BROWN_MUSHROOM)
                    .addIngredient(new RecipeChoice.MaterialChoice(Material.POPPY, Material.CORNFLOWER))
    ),

    FIREWORK_STAR(Material.FIREWORK_STAR, item ->
            new ShapelessRecipe(key("firework_star"), new ItemStack(Material.FIREWORK_STAR))
                    .addIngredient(Material.GUNPOWDER)
                    .addIngredient(new RecipeChoice.MaterialChoice(Material.RED_DYE, Material.BLUE_DYE))
    ),

    HONEY_BOTTLE(Material.HONEY_BOTTLE, item -> beehive("honey_bottle", Material.HONEY_BOTTLE, Material.GLASS_BOTTLE, "bottle", "honey")),

    HONEY_COMB(Material.HONEYCOMB, item -> beehive("honeycomb", Material.HONEYCOMB, Material.SHEARS, "shears", "honeycomb")),

    CONCRETE(item -> item.getType().name().endsWith("_CONCRETE"), item ->
            new ToolRecipe(key("concrete"), new ItemStack(item.getType()), Material.WOODEN_PICKAXE,
                    "&7Place the powder in water",
                    "&7to make it solid.",
                    "&7Concrete can be broken",
                    "&7with pickaxe.")
                    .addIngredient(Material.valueOf(item.getType().name() + "_POWDER"))
                    .addIngredient(Material.WATER_BUCKET)
    ),

    BUNDLE(item -> item.getType().name().endsWith("_BUNDLE"), item -> {
        Material bundle = item.getType();
        String colorName = bundle.name().replace("_BUNDLE", "");
        Material dye;

        try {
            dye = Material.valueOf(colorName + "_DYE");
        } catch (IllegalArgumentException e) {
            dye = Material.GRAY_DYE;
        }

        return new ShapelessRecipe(key("bundle"), new ItemStack(bundle))
                .addIngredient(new RecipeChoice.MaterialChoice(Material.BUNDLE, Material.WHITE_BUNDLE, Material.LIGHT_GRAY_BUNDLE, Material.GRAY_BUNDLE, Material.BLACK_BUNDLE, Material.BROWN_BUNDLE, Material.RED_BUNDLE, Material.ORANGE_BUNDLE, Material.YELLOW_BUNDLE, Material.LIME_BUNDLE, Material.GREEN_BUNDLE, Material.CYAN_BUNDLE, Material.LIGHT_BLUE_BUNDLE, Material.BLUE_BUNDLE, Material.PURPLE_BUNDLE, Material.MAGENTA_BUNDLE, Material.PINK_BUNDLE))
                .addIngredient(dye);
    }),

    STRIPPED_WOOD(item -> item.getType().name().startsWith("STRIPPED_"), item ->
            new ToolRecipe(key("stripped"), new ItemStack(item.getType()), Material.WOODEN_AXE,
                    "&7Right click block with",
                    "&7axe to make it stripped.")
                    .addIngredient(Material.valueOf(item.getType().name().replace("STRIPPED_", "")))
    ),

    MUD(Material.MUD, item ->
            new ToolRecipe(key("mud"), new ItemStack(Material.MUD), Material.POTION,
                    "&7Right click dirt with water",
                    "&7bottle to make it mud.")
                    .addIngredient(Material.DIRT)
    ),

    CARVED_PUMPKIN(Material.CARVED_PUMPKIN, item ->
            new ToolRecipe(key("carved_pumpkin"), new ItemStack(Material.CARVED_PUMPKIN), Material.SHEARS,
                    "&7Right click pumpkin with",
                    "&7shears to make it carved.")
                    .addIngredient(Material.PUMPKIN)
    ),

    WRITTEN_BOOK(Material.WRITTEN_BOOK, item ->
            new ToolRecipe(key("written_book"), new ItemStack(Material.WRITTEN_BOOK), Material.PAPER,
                    "&7Right Book and Quill,",
                    "&7sign and click Sign and close.")
                    .addIngredient(Material.WRITABLE_BOOK)
    ),

    CHIPPED_ANVIL(Material.CHIPPED_ANVIL, item ->
            new ToolRecipe(key("chipped_anvil"), new ItemStack(Material.CHIPPED_ANVIL), Material.RABBIT_FOOT,
                    "&7Drop anvil from X blocks",
                    "&7to make it chipped.")
                    .addIngredient(Material.ANVIL)
    ),

    DAMAGED_ANVIL(Material.DAMAGED_ANVIL, item ->
            new ToolRecipe(key("damaged_anvil"), new ItemStack(Material.DAMAGED_ANVIL), Material.RABBIT_FOOT,
                    "&7Drop anvil from Y blocks",
                    "&7to make it damaged.")
                    .addIngredient(Material.ANVIL)
    ),

    APPLE(Material.APPLE, item ->
            new ToolRecipe(key("apple"), new ItemStack(Material.APPLE), Material.WOODEN_HOE,
                    "&7Use hoe or fists",
                    "&7on leaves to get apple.")
                    .addIngredient(new RecipeChoice.MaterialChoice(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES))
    ),

    DRAGON_BREATH(Material.DRAGON_BREATH, item ->
            new ToolRecipe(key("dragon_breath"), new ItemStack(Material.DRAGON_BREATH), Material.GLASS_BOTTLE,
                    "&7Right click on dragon's breath",
                    "&7with empty bottle to",
                    "get dragon's breath.")
                    .addIngredient(Material.DRAGON_EGG)
    ),

    BONE_MEAL(Material.BONE_MEAL, item ->
            new ToolRecipe(key("bone_meal"), new ItemStack(Material.BONE_MEAL), Material.COMPOSTER,
                    "&7Right click with plants on",
                    "&7composter until it's full.")
                    .addIngredient(new RecipeChoice.MaterialChoice(Material.WHEAT_SEEDS, Material.ACACIA_LEAVES))
    ),

    WAXED(item -> item.getType().name().startsWith("WAXED_"), item ->
            new ToolRecipe(key("waxed"), new ItemStack(item.getType()), Material.HONEYCOMB,
                    "&7Sneak and Right click with",
                    "&7honeycomb on the placed block.",
                    "&7",
                    "&eNote! &7Wax can be removed",
                    "&7by right clicking block with any axe.")
                    .addIngredient(new RecipeChoice.MaterialChoice(Material.valueOf(item.getType().name().replaceFirst("WAXED_", ""))))
                    .addIngredient(new RecipeChoice.MaterialChoice(Material.HONEYCOMB))
    ),

    OXIDIZED(item -> item.getType().name().startsWith("EXPOSED_") ||
            item.getType().name().startsWith("WEATHERED_") ||
            item.getType().name().startsWith("OXIDIZED_"), item -> {

        Material normal = Material.valueOf(item.getType().name()
                .replaceFirst("EXPOSED_", "")
                .replaceFirst("WEATHERED_", "")
                .replaceFirst("OXIDIZED_", ""));

        return new ToolRecipe(key("oxidized"), new ItemStack(item.getType()), Material.CLOCK,
                "&7When placed, the copper block",
                "&7will oxidize over time.",
                "&7",
                "&7Stages:",
                "&6Normal &8-> &eExposed &8-> &aWeathered &8-> &2Oxidized",
                "&7",
                "&4IMPORTANT! &7Copper oxidizes faster",
                "&7if it is 4+ blocks away from any other copper",
                "&7",
                "&eNote! &7Oxidation stage can be reduced",
                "&7by right clicking block with any axe.")
                .addIngredient(new RecipeChoice.MaterialChoice(normal));
    }),

    ;

    private static final FakeRecipe[] CACHE = values();

    private final Predicate<ItemStack> itemMatcher;
    private final Function<ItemStack, Recipe> recipeSupplier;

    FakeRecipe(Material material, Function<ItemStack, Recipe> recipeSupplier) {
        this(item -> item.getType() == material, recipeSupplier);
    }

    FakeRecipe(Predicate<ItemStack> itemMatcher, Function<ItemStack, Recipe> recipeSupplier) {
        this.itemMatcher = itemMatcher;
        this.recipeSupplier = recipeSupplier;
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("fib", name);
    }

    private static Recipe beehive(String keyName, Material product, Material station, String toolName, String productName) {
        return new ToolRecipe(key(keyName), new ItemStack(product), station,
                "&7Right click a beehive with",
                "&fhoney_level: 5 &7with " + toolName,
                "&7to get " + productName,
                "",
                "&7When beehive is ready, you",
                "&7will see honey dripping",
                "&7from it. Press &fF3 &7and look",
                "&7at the beehive to check &fhoney_level",
                "&7(on the right side at the bottom)")
                .addIngredient(Material.BEEHIVE);
    }

    /** Plain entries apply whatever HARDER_TRACKERS says; a matching {@code _HARD} entry overrules them when it is on. */
    @Nullable
    public static FakeRecipe forItem(ItemStack item, GameSettings settings) {
        boolean harderTrackers = settings.isSettingEnabled(GameSetting.HARDER_TRACKERS);
        FakeRecipe plainMatch = null;

        for (FakeRecipe recipe : CACHE) {
            if (!recipe.itemMatcher.test(item)) {
                continue;
            }
            if (recipe.name().endsWith("_HARD")) {
                if (harderTrackers) {
                    return recipe;
                }
            } else if (plainMatch == null) {
                plainMatch = recipe;
            }
        }

        return plainMatch;
    }

    public Recipe getRecipe(ItemStack targetItem, Plugin plugin) {
        try {
            return recipeSupplier.apply(targetItem);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create recipe for " + targetItem, e);
            return null;
        }
    }
}
