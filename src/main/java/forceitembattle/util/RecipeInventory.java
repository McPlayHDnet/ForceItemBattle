package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RecipeInventory {

    /**
     * Player UUD -> remove on close.
     * If value is true, remove the key on inventory close, otherwise
     */
    private static HashMap<UUID, Boolean> crafting = new HashMap<>();

    public static boolean isShowingRecipe(Player player) {
        return crafting.containsKey(player.getUniqueId());
    }

    public static void handleRecipeClose(Player player) {
        Runnable closeHandler = closeHandlers.remove(player.getUniqueId());

        closeHandler.run();
    }

    /**
     * Player UUID -> handle inventory being closed by the player
     */
    public static HashMap<UUID, Runnable> closeHandlers = new HashMap<>();

    public static void showRecipe(Player player, ItemStack item) {
        if (Bukkit.getRecipesFor(item).isEmpty()) {
            player.sendMessage("§cThere is no recipe for this item. Just find it");
            return;
        }

        List<Inventory> inventories = createRecipeInventories(player, item);

        if (inventories.isEmpty()) {
            player.sendMessage("§cThere were recipes for this item that we cannot display, for some reason! §fTry /wikihelp");
            return;
        }

        if (closeHandlers.containsKey(player.getUniqueId())) {
            handleRecipeClose(player);
        }

        crafting.put(player.getUniqueId(), true);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ForceItemBattle.getInstance(), new Runnable() {

            private int inventoryIndex = 0;

            @Override
            public void run() {
                if (inventoryIndex >= inventories.size()) {
                    inventoryIndex = 0;
                }

                Inventory inventory = inventories.get(inventoryIndex);
                player.openInventory(inventory);


                inventoryIndex++;
            }
        }, 0, 40);

        closeHandlers.put(player.getUniqueId(), () -> {
            task.cancel();
            crafting.remove(player.getUniqueId());
        });

        return;
    }

    private static List<Inventory> createRecipeInventories(Player player, ItemStack item) {
        List<Inventory> inventories = new ArrayList<>();
        for (Recipe recipe : Bukkit.getServer().getRecipesFor(item)) {
            Inventory inventory = createRecipeInventory(item, recipe);

            if (inventory == null) {
                player.sendMessage("§cCould not find inventory for recipe type §f" + recipe.getClass().getSimpleName());
                continue;
            }
            inventories.add(inventory);
        }

        return inventories;
    }

    private static Inventory createRecipeInventory(ItemStack item, Recipe recipe) {
        Inventory inventory;
        List<ItemStack> ingredients = new ArrayList<>();

        if (recipe instanceof ShapedRecipe) {
            inventory = Bukkit.createInventory(null, InventoryType.WORKBENCH);

            CraftingInventory craftingInventory = (CraftingInventory) inventory;
            ShapedRecipe shaped = (ShapedRecipe) recipe;
            for (RecipeChoice recipeChoice : shaped.getChoiceMap().values()) {
                if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    materialChoice.getChoices().forEach(material -> {
                        ItemStack fixed = new ItemStack(material, 1);
                        ingredients.add(fixed);
                    });

                }
            }
            craftingInventory.setMatrix(ingredients.toArray(new ItemStack[0]));
            craftingInventory.setResult(item);

        } else if (recipe instanceof ShapelessRecipe) {
            inventory = Bukkit.createInventory(null, InventoryType.WORKBENCH);

            CraftingInventory craftingInventory = (CraftingInventory) inventory;
            ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
            for (RecipeChoice recipeChoice : shapeless.getChoiceList()) {
                if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    materialChoice.getChoices().forEach(material -> {
                        ItemStack fixed = new ItemStack(material, 1);
                        ingredients.add(fixed);
                    });

                }
            }
            craftingInventory.setMatrix(ingredients.toArray(new ItemStack[0]));
            craftingInventory.setResult(item);

        } else if (recipe instanceof FurnaceRecipe) {
            inventory = Bukkit.createInventory(null, InventoryType.FURNACE);

            FurnaceInventory furnaceInventory = (FurnaceInventory) inventory;
            FurnaceRecipe furnace = (FurnaceRecipe) recipe;
            ItemStack fixed = new ItemStack(furnace.getInput().getType(), 1, (byte) 0);
            ingredients.add(fixed);

            furnaceInventory.setSmelting(ingredients.get(0));
            furnaceInventory.setResult(item);

        } else if (recipe instanceof SmithingRecipe) {
            inventory = Bukkit.createInventory(null, InventoryType.SMITHING);

            SmithingInventory smithingInventory = (SmithingInventory) inventory;
            SmithingRecipe smithing = (SmithingRecipe) recipe;
            ingredients.add(smithing.getAddition().getItemStack());
            ingredients.add(smithing.getBase().getItemStack());

            int slot = 0;
            for (ItemStack ingredient : ingredients) {
                smithingInventory.setItem(slot, ingredient);
                slot++;
            }
            smithingInventory.setResult(item);
        } else {
            return null;
        }

        return inventory;
    }

    private static int convertItemIndexToInventorySlot(int firstItemSlot, int itemIndex) {
        return firstItemSlot + itemIndex / 3 + 9 * (itemIndex % 3);
    }
}
