package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.RecipeInventory;
import forceitembattle.util.RecipeViewer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.UUID;

public class RecipeManager {

    private final ForceItemBattle forceItemBattle;

    private final HashMap<UUID, RecipeViewer> recipeViewerMap;
    public final Map<UUID, Boolean> ignoreCloseHandler;
    public final Map<UUID, Runnable> closeHandlers;

    public RecipeManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.recipeViewerMap = new HashMap<>();
        this.ignoreCloseHandler = new HashMap<>();
        this.closeHandlers = new HashMap<>();
    }

    public void createRecipeViewer(Player player, ItemStack itemStack) {
        List<Recipe> recipes = getRecipes(itemStack);

        if (recipes.isEmpty()) {
            player.sendMessage("§cThere is no recipe for this item. Just find it lol");
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
        switch (item.getType()) {
            case FIREWORK_STAR -> {
                return getFireworkStarRecipes();
            }
            case SUSPICIOUS_STEW -> {
                return getSuspiciousStewRecipes();
            }
            default -> {
                return new ArrayList<>(Bukkit.getRecipesFor(item));
            }
        }

    }

    private List<Recipe> getFireworkStarRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        NamespacedKey key = NamespacedKey.fromString("forceitembattle:star", ForceItemBattle.getInstance());
        if (key == null) {
            return recipes;
        }

        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.FIREWORK_STAR));
        recipe.addIngredient(Material.GUNPOWDER);
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.RED_DYE, Material.BLUE_DYE));

        recipes.add(recipe);
        return recipes;
    }

    private List<Recipe> getSuspiciousStewRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        NamespacedKey key = NamespacedKey.fromString("forceitembattle:stew", ForceItemBattle.getInstance());
        if (key == null) {
            return recipes;
        }

        ShapelessRecipe recipe = new ShapelessRecipe(key, new ItemStack(Material.SUSPICIOUS_STEW));
        recipe.addIngredient(Material.RED_MUSHROOM);
        recipe.addIngredient(Material.BROWN_MUSHROOM);
        recipe.addIngredient(Material.BOWL);
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.POPPY, Material.CORNFLOWER));

        recipes.add(recipe);
        return recipes;
    }
}
