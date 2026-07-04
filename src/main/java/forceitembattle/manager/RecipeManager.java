package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.manager.customrecipe.FakeRecipe;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ItemBuilder;
import forceitembattle.util.RecipeInventory;
import forceitembattle.util.RecipeViewer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RecipeManager implements Manager {

    public final HashMap<UUID, Boolean> ignoreCloseHandler;
    public final HashMap<UUID, Runnable> closeHandlers;
    private final ForceItemBattle forceItemBattle;
    private final HashMap<UUID, RecipeViewer> recipeViewerMap;

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

    public void initRecipes() {
        final boolean easyRecipes = !this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.HARDER_TRACKERS);

        NamespacedKey antimatterKey = new NamespacedKey("fib", "antimatter_locator");
        ShapedRecipe antimatterRecipe = new ShapedRecipe(antimatterKey, new ItemBuilder(Material.KNOWLEDGE_BOOK).setDisplayName("<dark_gray>» <dark_purple>Antimatter Locator").getItemStack());
        if (easyRecipes) {
            antimatterRecipe.shape(" N ", "GQG", " N ");
            antimatterRecipe.setIngredient('N', Material.NETHER_BRICK);
            antimatterRecipe.setIngredient('G', Material.GLOWSTONE_DUST);
            antimatterRecipe.setIngredient('Q', Material.QUARTZ);
        } else {
            antimatterRecipe.shape("BGB", "QEQ", "BGB");
            antimatterRecipe.setIngredient('B', Material.NETHER_BRICK);
            antimatterRecipe.setIngredient('E', Material.ENDER_EYE);
            antimatterRecipe.setIngredient('G', Material.GLOWSTONE_DUST);
            antimatterRecipe.setIngredient('Q', Material.QUARTZ);
        }

        NamespacedKey chambersKey = new NamespacedKey("fib", "chambers_locator");
        ShapedRecipe chambersRecipe = new ShapedRecipe(chambersKey, new ItemBuilder(Material.WITHER_ROSE).setDisplayName("<dark_gray>» <gold>Trial Locator").getItemStack());
        if (easyRecipes) {
            chambersRecipe.shape("BGB", "GCG", "AAA");
            chambersRecipe.setIngredient('B', Material.CUT_COPPER);
            chambersRecipe.setIngredient('G', Material.GLASS);
            chambersRecipe.setIngredient('C', Material.COMPASS);
            chambersRecipe.setIngredient('A', Material.GOLD_INGOT);
        } else {
            chambersRecipe.shape("OKO", "GCI", "ODO");
            chambersRecipe.setIngredient('O', Material.OBSIDIAN);
            chambersRecipe.setIngredient('C', Material.COMPASS);
            chambersRecipe.setIngredient('K', Material.COPPER_INGOT);
            chambersRecipe.setIngredient('I', Material.IRON_INGOT);
            chambersRecipe.setIngredient('G', Material.GOLD_INGOT);
            chambersRecipe.setIngredient('D', Material.DIAMOND);
        }

        Bukkit.addRecipe(antimatterRecipe);
        Bukkit.addRecipe(chambersRecipe);
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
        FakeRecipe fakeRecipe = FakeRecipe.forItem(item, this.forceItemBattle);

        if (fakeRecipe != null) {
            Recipe recipe = fakeRecipe.getRecipe(item, this.forceItemBattle);

            if (recipe != null) {
                return List.of(recipe);
            }
        }

        return new ArrayList<>(Bukkit.getRecipesFor(item));
    }

}
