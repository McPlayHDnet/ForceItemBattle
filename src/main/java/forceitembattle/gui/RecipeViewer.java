package forceitembattle.gui;

import java.util.List;
import java.util.UUID;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RecipeViewer {

    private final int pages;
    private final List<Recipe> recipes;
    @Setter
    private UUID uuid;
    @Setter
    private ItemStack itemStack;
    @Setter
    private Recipe recipe;
    @Setter
    private int currentRecipeIndex;

    public RecipeViewer(List<Recipe> recipes) {
        this.recipes = recipes;
        this.pages = recipes.size();
    }

    public List<Recipe> recipes() {
        return recipes;
    }

    public UUID uuid() {
        return uuid;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public Recipe recipe() {
        return recipe;
    }

    public int currentRecipeIndex() {
        return currentRecipeIndex;
    }

    public int pages() {
        return pages;
    }
}
