package forceitembattle.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.UUID;

public class RecipeViewer {

    private UUID uuid;
    private ItemStack itemStack;
    private Recipe recipe;
    private int currentRecipeIndex;
    private final int pages;

    private final List<Recipe> recipes;

    public RecipeViewer(List<Recipe> recipes) {
        this.recipes = recipes;
        this.pages = recipes.size();
    }

    /**
     * All recipes for this item.
     */
    public List<Recipe> recipes() {
        return recipes;
    }

    public UUID uuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /**
     * Currently viewed recipe.
     */
    public Recipe recipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public int currentRecipeIndex() {
        return currentRecipeIndex;
    }

    public void setCurrentRecipeIndex(int recipeIndex) {
        this.currentRecipeIndex = recipeIndex;
    }

    /**
     * Total amount of recipes for this item.
     */
    public int pages() {
        return pages;
    }
}
