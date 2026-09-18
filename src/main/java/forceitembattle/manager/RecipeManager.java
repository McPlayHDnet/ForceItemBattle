package forceitembattle.manager;

import forceitembattle.gui.RecipeInventory;
import forceitembattle.gui.RecipeViewer;
import forceitembattle.manager.customrecipe.FakeRecipe;
import forceitembattle.model.CustomMaterials;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeManager implements Manager {

    private final GameSettings settings;
    public final HashMap<UUID, Boolean> ignoreCloseHandler;
    public final HashMap<UUID, Runnable> closeHandlers;
    private final Plugin plugin;
    private final HashMap<UUID, RecipeViewer> recipeViewerMap;

    public RecipeManager(Plugin plugin, GameSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.recipeViewerMap = new HashMap<>();
        this.ignoreCloseHandler = new HashMap<>();
        this.closeHandlers = new HashMap<>();
    }

    public void createRecipeViewer(Player player, ItemStack itemStack) {
        List<Recipe> recipes = new ArrayList<>(getRecipes(itemStack));
        recipes.removeIf(recipe -> RecipeInventory.getStationItem(recipe) == null);

        if (recipes.isEmpty()) {
            player.sendMessage(Text.of("<red>There is no recipe for this item. Just find it lol"));
            return;
        }

        RecipeViewer recipeViewer = new RecipeViewer(recipes);
        recipeViewer.setUuid(player.getUniqueId());
        recipeViewer.setItemStack(itemStack);
        recipeViewer.setCurrentRecipeIndex(0);
        recipeViewer.setRecipe(recipes.get(0));

        this.recipeViewerMap.put(player.getUniqueId(), recipeViewer);

        new RecipeInventory(this, this.getRecipeViewer(player), player).open(player);
    }

    public void initRecipes() {
        final boolean easyRecipes = !this.settings.isSettingEnabled(GameSetting.HARDER_TRACKERS);

        NamespacedKey antimatterKey = new NamespacedKey("fib", "antimatter_locator");
        ShapedRecipe antimatterRecipe = new ShapedRecipe(antimatterKey, CustomMaterials.ANTIMATTER_LOCATOR.itemStack());
        antimatterRecipe.shape(" B ", "GQG", " B ");
        antimatterRecipe.setIngredient('B', Material.NETHER_BRICK);
        antimatterRecipe.setIngredient('G', Material.GLOWSTONE_DUST);
        antimatterRecipe.setIngredient('Q', Material.QUARTZ);

        NamespacedKey chambersKey = new NamespacedKey("fib", "chambers_locator");
        ShapedRecipe chambersRecipe = new ShapedRecipe(chambersKey, CustomMaterials.TRIAL_LOCATOR.itemStack());
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

        NamespacedKey totemKey = new NamespacedKey("fib", "totem_of_antimatter");
        ShapedRecipe totemRecipe = new ShapedRecipe(totemKey, CustomMaterials.TOTEM_OF_ANTIMATTER.itemStack());
        totemRecipe.shape(" E ", "QGQ", " Q ");
        totemRecipe.setIngredient('E', RecipeChoice.exactChoice(CustomMaterials.EYE_OF_ANTIMATTER.itemStack()));
        totemRecipe.setIngredient('G', Material.GLOWSTONE);
        totemRecipe.setIngredient('Q', Material.QUARTZ);

        reRegister(antimatterKey, antimatterRecipe);
        reRegister(chambersKey, chambersRecipe);
        reRegister(totemKey, totemRecipe);
    }

    /**
     * Registers a recipe, replacing any earlier one under the same key.
     *
     * <p>{@link #initRecipes()} runs from {@code startGame()} on every round, and
     * {@code Bukkit.addRecipe} throws {@code Duplicate recipe ignored} for a key already registered
     * — which aborts the second round of a server session before anything else happens. Production
     * never sees it because {@code scheduleReset} restarts the JVM between rounds.
     *
     * <p>Removing rather than skipping is deliberate: the tracker shapes depend on the
     * HARDER_TRACKERS setting, which can change between rounds, so they have to be rebuilt.
     */
    private void reRegister(NamespacedKey key, Recipe recipe) {
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
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
        FakeRecipe fakeRecipe = FakeRecipe.forItem(item, this.settings);

        if (fakeRecipe != null) {
            Recipe recipe = fakeRecipe.getRecipe(item, this.plugin);

            if (recipe != null) {
                return List.of(recipe);
            }
        }

        return new ArrayList<>(Bukkit.getRecipesFor(item));
    }

}
