package forceitembattle.manager.customrecipe;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.*;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

public enum FakeRecipe {

    END_STRUCTURE(Material.KNOWLEDGE_BOOK, item ->
            RecipeBuilder.newBuilder(ShapedRecipe::new)
                    .apply(shapedRecipe -> shapedRecipe.shape(" N ", "GQG", " N "))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('N', Material.NETHER_BRICK))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('G', Material.GLOWSTONE_DUST))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('Q', Material.QUARTZ))
                    .build("fib:antimatter_locator", new ItemBuilder(Material.KNOWLEDGE_BOOK).setDisplayName("<dark_gray>» <dark_purple>Antimatter Locator").getItemStack())
    ),

    END_STRUCTURE_HARD(Material.KNOWLEDGE_BOOK, item ->
            RecipeBuilder.newBuilder(ShapedRecipe::new)
                    .apply(shapedRecipe -> shapedRecipe.shape("BGB", "QEQ", "BGB"))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('B', Material.NETHER_BRICK))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('E', Material.ENDER_EYE))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('G', Material.GLOWSTONE_DUST))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('Q', Material.QUARTZ))
                    .build("fib:antimatter_locator", new ItemBuilder(Material.KNOWLEDGE_BOOK).setDisplayName("<dark_gray>» <dark_purple>Antimatter Locator").getItemStack())
    ),

    CHAMBER_STRUCTURE(Material.WITHER_ROSE, item ->
            RecipeBuilder.newBuilder(ShapedRecipe::new)
                    .apply(shapedRecipe -> shapedRecipe.shape("BGB", "GCG", "AAA"))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('B', Material.CUT_COPPER))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('G', Material.GLASS))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('C', Material.COMPASS))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('A', Material.GOLD_INGOT))
                    .build("fib:chambers_locator", new ItemBuilder(Material.WITHER_ROSE).setDisplayName("<dark_gray>» <gold>Trial Locator").getItemStack())
    ),

    CHAMBER_STRUCTURE_HARD(Material.WITHER_ROSE, item ->
            RecipeBuilder.newBuilder(ShapedRecipe::new)
                    .apply(shapedRecipe -> shapedRecipe.shape("OKO", "GCI", "ODO"))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('O', Material.OBSIDIAN))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('C', Material.COMPASS))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('K', Material.COPPER_INGOT))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('I', Material.IRON_INGOT))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('G', Material.GOLD_INGOT))
                    .apply(shapedRecipe -> shapedRecipe.setIngredient('D', Material.DIAMOND))
                    .build("fib:chambers_locator", new ItemBuilder(Material.WITHER_ROSE).setDisplayName("<dark_gray>» <gold>Trial Locator").getItemStack())
    ),

    SUSPICIOUS_STEW(Material.SUSPICIOUS_STEW, item ->
            RecipeBuilder.newBuilder(ShapelessRecipe::new)
                    .apply(recipe -> recipe.addIngredient(Material.BOWL))
                    .apply(recipe -> recipe.addIngredient(Material.RED_MUSHROOM))
                    .apply(recipe -> recipe.addIngredient(Material.BROWN_MUSHROOM))
                    .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.POPPY, Material.CORNFLOWER)))
                    .build("fib:suspicious_stew", new ItemStack(Material.SUSPICIOUS_STEW))
    ),

    FIREWORK_STAR(Material.FIREWORK_STAR, item ->
            RecipeBuilder.newBuilder(ShapelessRecipe::new)
                    .apply(recipe -> recipe.addIngredient(Material.GUNPOWDER))
                    .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.RED_DYE, Material.BLUE_DYE)))
                    .build("fib:firework_star", new ItemStack(Material.FIREWORK_STAR))
    ),

    HONEY_BOTTLE(Material.HONEY_BOTTLE, item ->
            RecipeBuilder.newBuilder(ToolRecipe::new)
                    .apply(recipe -> recipe.addIngredient(Material.BEEHIVE))
                    .apply(recipe -> recipe.addInteractionLore(
                            "&7Right click a beehive with",
                            "&fhoney_level: 5 &7with bottle",
                            "&7to get honey",
                            "",
                            "&7When beehive is ready, you",
                            "&7will see honey dripping",
                            "&7from it. Press &fF3 &7and look",
                            "&7at the beehive to check &fhoney_level",
                            "&7(on the right side at the bottom)"
                    ))
                    .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.GLASS_BOTTLE)))
                    .build("fib:honey_bottle", new ItemStack(Material.HONEY_BOTTLE))
    ),

    HONEY_COMB(Material.HONEYCOMB, item ->
            RecipeBuilder.newBuilder(ToolRecipe::new)
                    .apply(recipe -> recipe.addIngredient(Material.BEEHIVE))
                    .apply(recipe -> recipe.addInteractionLore(
                            "&7Right click a beehive with",
                            "&fhoney_level: 5 &7with shears",
                            "&7to get honeycomb",
                            "",
                            "&7When beehive is ready, you",
                            "&7will see honey dripping",
                            "&7from it. Press &fF3 &7and look",
                            "&7at the beehive to check &fhoney_level",
                            "&7(on the right side at the bottom)"
                    ))
                    .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.SHEARS)))
                    .build("fib:honeycomb", new ItemStack(Material.HONEYCOMB))
    ),

    CONCRETE(item -> item.getType().name().endsWith("_CONCRETE"), item -> {
        Material concretePowder = Material.valueOf(item.getType().name() + "_POWDER");

        return RecipeBuilder.newBuilder(ToolRecipe::new)
                .apply(recipe -> recipe.addIngredient(concretePowder))
                .apply(recipe -> recipe.addIngredient(Material.WATER_BUCKET))
                .apply(recipe -> recipe.addInteractionLore(
                        "&7Place the powder in water",
                        "&7to make it solid.",
                        "&7Concrete can be broken",
                        "&7with pickaxe."
                ))
                .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.WOODEN_PICKAXE)))
                .build("fib:concrete", new ItemStack(item.getType()));
    }),

    BUNDLE(item -> item.getType().name().endsWith("_BUNDLE"), item -> {
        Material bundle = item.getType();
        String colorName = bundle.name().replace("_BUNDLE", "");
        Material dye;

        try {
            dye = Material.valueOf(colorName + "_DYE");
        } catch (IllegalArgumentException e) {
            dye = Material.GRAY_DYE; // Default color ig
        }

        Material finalDye = dye;
        return RecipeBuilder.newBuilder(ShapelessRecipe::new)
                .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.BUNDLE, Material.WHITE_BUNDLE, Material.LIGHT_GRAY_BUNDLE, Material.GRAY_BUNDLE, Material.BLACK_BUNDLE, Material.BROWN_BUNDLE, Material.RED_BUNDLE, Material.ORANGE_BUNDLE, Material.YELLOW_BUNDLE, Material.LIME_BUNDLE, Material.GREEN_BUNDLE, Material.CYAN_BUNDLE, Material.LIGHT_BLUE_BUNDLE, Material.BLUE_BUNDLE, Material.PURPLE_BUNDLE, Material.MAGENTA_BUNDLE, Material.PINK_BUNDLE)))
                .apply(recipe -> recipe.addIngredient(finalDye))
                .build("fib:bundle", new ItemStack(bundle));
    }),

    STRIPPED_WOOD(item -> item.getType().name().startsWith("STRIPPED_"), item -> {
        Material fullWood = Material.valueOf(item.getType().name().replace("STRIPPED_", ""));

        return RecipeBuilder.newBuilder(ToolRecipe::new)
                .apply(recipe -> recipe.addIngredient(fullWood))
                .apply(recipe -> recipe.addInteractionLore(
                        "&7Right click block with",
                        "&7axe to make it stripped."
                ))
                .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.WOODEN_AXE)))
                .build("fib:stripped", new ItemStack(item.getType()));
    }),

    MUD(Material.MUD, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.DIRT))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right click dirt with water",
                    "&7bottle to make it mud."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.POTION)))
            .build("fib:mud", new ItemStack(Material.MUD))
    ),

    CARVED_PUMPKIN(Material.CARVED_PUMPKIN, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.PUMPKIN))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right click pumpkin with",
                    "&7shears to make it carved."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.SHEARS)))
            .build("fib:carved_pumpkin", new ItemStack(Material.CARVED_PUMPKIN))
    ),

    WRITTEN_BOOK(Material.WRITTEN_BOOK, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.WRITABLE_BOOK))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right Book and Quill,",
                    "&7sign and click Sign and close."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.PAPER)))
            .build("fib:written_book", new ItemStack(Material.WRITTEN_BOOK))
    ),

    CHIPPED_ANVIL(Material.CHIPPED_ANVIL, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.ANVIL))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Drop anvil from X blocks",
                    "&7to make it chipped."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.RABBIT_FOOT)))
            .build("fib:chipped_anvil", new ItemStack(Material.CHIPPED_ANVIL))
    ),

    DAMAGED_ANVIL(Material.DAMAGED_ANVIL, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.ANVIL))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Drop anvil from Y blocks",
                    "&7to make it damaged."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.RABBIT_FOOT)))
            .build("fib:damaged_anvil", new ItemStack(Material.DAMAGED_ANVIL))
    ),

    APPLE(Material.APPLE, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES)))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Use hoe or fists",
                    "&7on leaves to get apple."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.WOODEN_HOE)))
            .build("fib:apple", new ItemStack(Material.APPLE))
    ),

    DRAGON_BREATH(Material.DRAGON_BREATH, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.DRAGON_EGG))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right click on dragon's breath",
                    "&7with empty bottle to",
                    "get dragon's breath."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.GLASS_BOTTLE)))
            .build("fib:dragon_breath", new ItemStack(Material.DRAGON_BREATH))
    ),

    BONE_MEAL(Material.BONE_MEAL, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.WHEAT_SEEDS, Material.ACACIA_LEAVES)))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right click with plants on",
                    "&7composter until it's full."
            ))
            .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.COMPOSTER)))
            .build("fib:bone_meal", new ItemStack(Material.BONE_MEAL))
    ),

    WAXED(item -> item.getType().name().startsWith("WAXED_"), item -> {
        Material unwaxed = Material.valueOf(item.getType().name().replaceFirst("WAXED_", ""));

        return RecipeBuilder.newBuilder(ToolRecipe::new)
                .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(unwaxed)))
                .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.HONEYCOMB)))
                .apply(recipe -> recipe.addInteractionLore(
                        "&7Sneak and Right click with",
                        "&7honeycomb on the placed block.",
                        "&7",
                        "&eNote! &7Wax can be removed",
                        "&7by right clicking block with any axe."
                ))
                .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.HONEYCOMB)))
                .build("fib:waxed", new ItemStack(item.getType()));
        }
    ),

    OXIDIZED(item -> item.getType().name().startsWith("EXPOSED_") ||
            item.getType().name().startsWith("WEATHERED_") ||
            item.getType().name().startsWith("OXIDIZED_"), item -> {

        Material normal = Material.valueOf(item.getType().name()
                .replaceFirst("EXPOSED_", "")
                .replaceFirst("WEATHERED_", "")
                .replaceFirst("OXIDIZED_", ""));

        return RecipeBuilder.newBuilder(ToolRecipe::new)
                .apply(recipe -> recipe.addIngredient(new RecipeChoice.MaterialChoice(normal)))
                .apply(recipe -> recipe.addInteractionLore(
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
                        "&7by right clicking block with any axe."
                ))
                .apply(recipe -> recipe.setStationDisplay(new ItemStack(Material.CLOCK)))
                .build("fib:oxidized", new ItemStack(item.getType()));
        }
    ),

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

    public Recipe getRecipe(ItemStack targetItem) {
        try {
            return recipeSupplier.apply(targetItem);
        } catch (Exception e) {
            ForceItemBattle.getInstance().getLogger().log(Level.WARNING, "Failed to create recipe for " + targetItem, e);
            return null;
        }
    }

    @Nullable
    public static FakeRecipe forItem(ItemStack item) {
        for (FakeRecipe recipe : CACHE) {
            if (recipe.itemMatcher.test(item)) {
                boolean isRecipeHard = recipe.name().endsWith("_HARD");
                if (isRecipeHard == ForceItemBattle.getInstance().getSettings().isSettingEnabled(GameSetting.HARDER_TRACKERS)) {
                    return recipe;
                }
            }
        }
        return null;
    }
}
