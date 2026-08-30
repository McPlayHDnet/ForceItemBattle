package forceitembattle.manager;

import forceitembattle.settings.GameSettings;
import forceitembattle.ForceItemBattle;
import forceitembattle.manager.customrecipe.FakeRecipe;
import forceitembattle.model.CustomMaterials;
import forceitembattle.settings.GameSetting;
import forceitembattle.gui.ItemBuilder;
import forceitembattle.gui.RecipeInventory;
import forceitembattle.gui.RecipeViewer;
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

public class RecipeManager implements Manager {

    private final GameSettings settings;
    public final HashMap<UUID, Boolean> ignoreCloseHandler;
    public final HashMap<UUID, Runnable> closeHandlers;
    private final ForceItemBattle forceItemBattle;
    private final HashMap<UUID, RecipeViewer> recipeViewerMap;

    public RecipeManager(ForceItemBattle forceItemBattle, GameSettings settings) {
        this.forceItemBattle = forceItemBattle;
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

        new RecipeInventory(this.forceItemBattle, this.getRecipeViewer(player), player).open(player);
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
        totemRecipe.setIngredient('E', new RecipeChoice.ExactChoice(CustomMaterials.EYE_OF_ANTIMATTER.itemStack()));
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
     * {@code Bukkit.addRecipe} throws {@code IllegalStateException: Duplicate recipe ignored} for a
     * key that is already registered. Since {@code initRecipes()} is the first thing
     * {@code startGame()} does, that used to abort the second round of a server session before any
     * of it happened: no player setup, no state change to MID_GAME, no timer — everyone left
     * standing in creative with no indication anything had gone wrong. It went unnoticed because
     * {@code scheduleReset} restarts the JVM between rounds in production, which clears the
     * registry the hard way.
     *
     * <p>Removing rather than skipping is deliberate: the tracker shapes depend on the
     * HARDER_TRACKERS setting, which can be changed between rounds, so they have to be rebuilt from
     * the current settings rather than left as whatever the last round registered.
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
