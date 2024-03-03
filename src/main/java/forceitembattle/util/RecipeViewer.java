package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.UUID;

@Getter
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
}
