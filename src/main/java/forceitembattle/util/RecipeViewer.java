package forceitembattle.util;

import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.UUID;

public class RecipeViewer {

    @Setter
    private UUID uuid;
    @Setter
    private ItemStack itemStack;
    @Setter
    private Recipe recipe;
    @Setter
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

    public ItemStack itemStack() {
        return itemStack;
    }

    /**
     * Currently viewed recipe.
     */
    public Recipe recipe() {
        return recipe;
    }

    public int currentRecipeIndex() {
        return currentRecipeIndex;
    }

    /**
     * Total amount of recipes for this item.
     */
    public int pages() {
        return pages;
    }
}
