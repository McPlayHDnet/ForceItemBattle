package forceitembattle.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.*;

import java.util.UUID;

public class RecipeViewer {

    private UUID uuid;
    private ItemStack itemStack;
    private Recipe recipe;
    private int currentRecipeIndex;
    private int pages;

    public RecipeViewer() {

    }

    private int recipes() {
        return Bukkit.getRecipesFor(this.itemStack()).size();
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

    public int pages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
