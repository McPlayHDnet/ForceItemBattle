package forceitembattle.manager.customrecipe;

import forceitembattle.ForceItemBattle;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

public enum FakeRecipe {

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
                    .apply(recipe -> recipe.addIngredient(Material.GLASS_BOTTLE))
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
                    .build("fib:honey_bottle", new ItemStack(Material.HONEY_BOTTLE))
    ),

    HONEY_COMB(Material.HONEYCOMB, item ->
            RecipeBuilder.newBuilder(ToolRecipe::new)
                    .apply(recipe -> recipe.addIngredient(Material.SHEARS))
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
                    .build("fib:honeycomb", new ItemStack(Material.HONEYCOMB))
    ),

    CONCRETE(item -> item.getType().name().endsWith("_CONCRETE"), item -> {
        Material concretePowder = Material.valueOf(item.getType().name() + "_POWDER");

        return RecipeBuilder.newBuilder(ToolRecipe::new)
                .apply(recipe -> recipe.addIngredient(concretePowder))
                .apply(recipe -> recipe.addIngredient(Material.WATER_BUCKET))
                .apply(recipe -> recipe.addInteractionLore(
                        "&7Place the powder in water",
                        "&7to make it solid."
                ))
                .build("fib:concrete", new ItemStack(item.getType()));
    }),

    STRIPPED_WOOD(item -> item.getType().name().startsWith("STRIPPED_"), item -> {
        Material fullWood = Material.valueOf(item.getType().name().replace("STRIPPED_", ""));

        return RecipeBuilder.newBuilder(ToolRecipe::new)
                .apply(recipe -> recipe.addIngredient(fullWood))
                .apply(recipe -> recipe.addIngredient(Material.WATER_BUCKET))
                .apply(recipe -> recipe.addInteractionLore(
                        "&7Right click block with",
                        "&7axe to make it stripped."
                ))
                .build("fib:stripped", new ItemStack(item.getType()));
    }),

    MUD(Material.DIRT, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.DIRT))
            .apply(recipe -> recipe.addIngredient(Material.POTION))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right click dirt with water",
                    "&7bottle to make it mud."
            ))
            .build("fib:mud", new ItemStack(Material.MUD))
    ),

    CARVED_PUMPKIN(Material.CARVED_PUMPKIN, item -> RecipeBuilder.newBuilder(ToolRecipe::new)
            .apply(recipe -> recipe.addIngredient(Material.PUMPKIN))
            .apply(recipe -> recipe.addIngredient(Material.SHEARS))
            .apply(recipe -> recipe.addInteractionLore(
                    "&7Right click pumpkin with",
                    "&7shears to make it carved."
            ))
            .build("fib:carved_pumpkin", new ItemStack(Material.CARVED_PUMPKIN))
    ),

    // TODO : Written book,
    //  Chipped anvil,
    //  Damaged anvil (say how long the drop needs to be to turn damaged),
    //  Apple (show hoe as a tool for leaves) (might be unnecessary),
    //  Dragon's breath

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
                return recipe;
            }
        }
        return null;
    }
}
