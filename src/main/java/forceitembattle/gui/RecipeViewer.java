package forceitembattle.gui;

import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RecipeViewer {

    private final UUID uuid;
    private final ItemStack itemStack;
    private final List<Recipe> recipes;
    private int currentRecipeIndex;

    public RecipeViewer(UUID uuid, ItemStack itemStack, List<Recipe> recipes) {
        this.uuid = uuid;
        this.itemStack = itemStack;
        this.recipes = recipes;
    }

    /** Moves by {@code delta} pages; false, and nothing moves, when that would run off either end. */
    public boolean turn(int delta) {
        int next = this.currentRecipeIndex + delta;
        if (next < 0 || next >= this.pages()) {
            return false;
        }
        this.currentRecipeIndex = next;
        return true;
    }

    public UUID uuid() {
        return uuid;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public Recipe recipe() {
        return recipes.get(currentRecipeIndex);
    }

    public int currentRecipeIndex() {
        return currentRecipeIndex;
    }

    public int pages() {
        return recipes.size();
    }
}
