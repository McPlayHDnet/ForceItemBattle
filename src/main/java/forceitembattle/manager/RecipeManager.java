package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.customrecipe.FakeRecipe;
import forceitembattle.util.RecipeInventory;
import forceitembattle.util.RecipeViewer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RecipeManager {

    private final ForceItemBattle forceItemBattle;

    private final HashMap<UUID, RecipeViewer> recipeViewerMap;
    public final HashMap<UUID, Boolean> ignoreCloseHandler;
    public final HashMap<UUID, Runnable> closeHandlers;

    public RecipeManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.recipeViewerMap = new HashMap<>();
        this.ignoreCloseHandler = new HashMap<>();
        this.closeHandlers = new HashMap<>();
    }

    public void createRecipeViewer(Player player, ItemStack itemStack) {
        List<Recipe> recipes = new ArrayList<>(getRecipes(itemStack));
        recipes.removeIf(recipe -> RecipeInventory.getStationItem(recipe) == null);

        if (recipes.isEmpty()) {
            player.sendMessage(this.forceItemBattle.getGamemanager().getMiniMessage().deserialize("<red>There is no recipe for this item. Just find it lol"));
            return;
        }

        RecipeViewer recipeViewer = new RecipeViewer(recipes);
        recipeViewer.setUuid(player.getUniqueId());
        recipeViewer.setItemStack(itemStack);
        recipeViewer.setCurrentRecipeIndex(0);
        recipeViewer.setRecipe(recipes.get(0));

        this.recipeViewerMap.put(player.getUniqueId(), recipeViewer);

        new RecipeInventory(this.forceItemBattle, this.forceItemBattle.getRecipeManager().getRecipeViewer(player), player).open(player);
    }

    public boolean ignoreInventoryClosed(Player player) {
        return ignoreCloseHandler.getOrDefault(player.getUniqueId(), false);
    }

    public boolean isShowingRecipe(Player player) {
        return closeHandlers.containsKey(player.getUniqueId());
    }

    public void handleRecipeClose(Player player) {
        Runnable closeHandler = closeHandlers.remove(player.getUniqueId());
        ignoreCloseHandler.remove(player.getUniqueId());
        closeHandler.run();
    }

    public RecipeViewer getRecipeViewer(Player player) {
        return this.recipeViewerMap.get(player.getUniqueId());
    }

    public List<Recipe> getRecipes(ItemStack item) {
        FakeRecipe fakeRecipe = FakeRecipe.forItem(item);

        if (fakeRecipe != null) {
            Recipe recipe = fakeRecipe.getRecipe(item);

            if (recipe != null) {
                return List.of(recipe);
            }
        }

        return new ArrayList<>(Bukkit.getRecipesFor(item));
    }

}
