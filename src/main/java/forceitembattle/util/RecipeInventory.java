package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("duplicate")
public class RecipeInventory {

    /**
     * Player UUD -> remove on close.
     * If value is true, ignore close inventory
     */
    private static final HashMap<UUID, Boolean> ignoreCloseHandler = new HashMap<>();

    public static boolean ignoreInventoryClosed(Player player) {
        return ignoreCloseHandler.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean isShowingRecipe(Player player) {
        return closeHandlers.containsKey(player.getUniqueId());
    }

    public static void handleRecipeClose(Player player) {
        Runnable closeHandler = closeHandlers.remove(player.getUniqueId());
        ignoreCloseHandler.remove(player.getUniqueId());

        closeHandler.run();
    }

    /**
     * Player UUID -> handle inventory being closed by the player
     */
    private static final HashMap<UUID, Runnable> closeHandlers = new HashMap<>();

    public static void showRecipe(Player player, ItemStack item) {
        if (Bukkit.getRecipesFor(item).isEmpty()) {
            player.sendMessage("§cThere is no recipe for this item. Just find it");
            return;
        }

        List<Inventory> inventories = createInventories(player, item);

        if (inventories.isEmpty()) {
            player.sendMessage("§cThere were recipes for this item that we cannot display, for some reason! §fTry /infowiki");
            return;
        }

        if (closeHandlers.containsKey(player.getUniqueId())) {
            handleRecipeClose(player);
        }

        ignoreCloseHandler.put(player.getUniqueId(), false);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(ForceItemBattle.getInstance(), new Runnable() {

            private int inventoryIndex = 0;

            @Override
            public void run() {
                if (inventoryIndex >= inventories.size()) {
                    inventoryIndex = 0;
                }

                Inventory inventory = inventories.get(inventoryIndex);
                ignoreCloseHandler.put(player.getUniqueId(), true);
                player.openInventory(inventory);

                ignoreCloseHandler.put(player.getUniqueId(), false);

                inventoryIndex++;
            }
        }, 0, 20);

        closeHandlers.put(player.getUniqueId(), () -> {
            task.cancel();
            ignoreCloseHandler.remove(player.getUniqueId());
        });
    }

    private static List<Inventory> createInventories(Player player, ItemStack item) {
        List<Inventory> inventories = new ArrayList<>();
        for (Recipe recipe : Bukkit.getServer().getRecipesFor(item)) {
            Inventory inventory = createFancyRecipeInventory(item, recipe);

            if (inventory == null) {
                player.sendMessage("§cCould not find inventory for recipe type §f" + recipe.getClass().getSimpleName());
                continue;
            }
            inventories.add(inventory);
        }

        return inventories;
    }
    /*
Slots visualisation for values below:

 0  1  2  3  4  5  6  7  8
 9 XX XX XX 13 14 15 16 17
18 XX SS XX 22 ST 24 RS 26
27 XX XX XX 31 32 33 34 35
36 37 38 39 40 41 42 43 44

 */
    private static final int RESULT_SLOT = 25;
    private static final int STATION_SLOT = 23;
    private static final int WORKBENCH_FIRST_ITEM_SLOT = 10;
    private static final int SMITHING_FIRST_ITEM_SLOT = 19;
    private static final int OTHER_FIRST_ITEM_SLOT = 20;

    private static Inventory createFancyRecipeInventory(ItemStack item, Recipe recipe) {
        String itemName = WordUtils.capitalize(item.getType().name().replace("_", " ").toLowerCase());
        Inventory inventory = Bukkit.createInventory(null, 5 * 9,  itemName);

        List<ItemStack> ingredients = new ArrayList<>();

        if (recipe instanceof ShapedRecipe shaped) {
            String[] shape = shaped.getShape();

            int rowIndex = 0;
            for (String row : shape) {
                int charIndex = 0;
                for (char c : row.toCharArray()) {
                    int slot = WORKBENCH_FIRST_ITEM_SLOT + rowIndex * 9 + charIndex;

                    RecipeChoice choice = shaped.getChoiceMap().get(c);
                    if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                        ItemStack fixed = new ItemStack(materialChoice.getChoices().get(0), 1);

                        inventory.setItem(slot, fixed);
                    } else if (choice != null) {
                        inventory.setItem(slot, new ItemStack(choice.getItemStack()));
                    }
                    charIndex++;
                }
                rowIndex++;
            }

        } else if (recipe instanceof ShapelessRecipe shapeless) {
            for (RecipeChoice recipeChoice : shapeless.getChoiceList()) {
                if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    materialChoice.getChoices().forEach(material -> {
                        ItemStack fixed = new ItemStack(material, 1);
                        ingredients.add(fixed);
                    });

                }
            }

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                inventory.setItem(convertItemIndexToInventorySlot(WORKBENCH_FIRST_ITEM_SLOT, index), ingredient);
                index++;
            }
        } else if (recipe instanceof CookingRecipe<?> furnace) {
            ItemStack fixed = new ItemStack(furnace.getInput().getType(), 1);

            inventory.setItem(OTHER_FIRST_ITEM_SLOT, fixed);

        } else if (recipe instanceof SmithingTrimRecipe smithing) {
            ingredients.add(smithing.getBase().getItemStack());
            ingredients.add(smithing.getTemplate().getItemStack());
            ingredients.add(smithing.getAddition().getItemStack());

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                inventory.setItem(OTHER_FIRST_ITEM_SLOT + index, ingredient);
                index++;
            }

        }  else if (recipe instanceof SmithingTransformRecipe smithing) {
            ingredients.add(smithing.getBase().getItemStack());
            ingredients.add(smithing.getTemplate().getItemStack());
            ingredients.add(smithing.getAddition().getItemStack());

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                inventory.setItem(SMITHING_FIRST_ITEM_SLOT + index, ingredient);
                index++;
            }

        } else if (recipe instanceof SmithingRecipe smithing) {
            // Unknown smithing recipe?
            ingredients.add(smithing.getAddition().getItemStack());
            ingredients.add(smithing.getBase().getItemStack());

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                inventory.setItem(SMITHING_FIRST_ITEM_SLOT + index, ingredient);
                index++;
            }

        } else if (recipe instanceof MerchantRecipe merchant) {
            ItemStack fixed = new ItemStack(merchant.getResult().getType(), 1, (byte) 0);
            ingredients.add(fixed);

            inventory.setItem(OTHER_FIRST_ITEM_SLOT, fixed);

        } else if (recipe instanceof StonecuttingRecipe stonecutting) {
            ItemStack fixed = new ItemStack(stonecutting.getInput());
            ingredients.add(fixed);

            inventory.setItem(OTHER_FIRST_ITEM_SLOT, fixed);
        } else {
            return null;
        }

        inventory.setItem(RESULT_SLOT, new ItemStack(item.getType(), 1));
        inventory.setItem(STATION_SLOT, getStationItem(recipe));

        return inventory;
    }

    private static ItemStack getStationItem(Recipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return new ItemStack(Material.CRAFTING_TABLE);
        } else if (recipe instanceof ShapelessRecipe) {
            return new ItemStack(Material.CRAFTING_TABLE);
        } else if (recipe instanceof FurnaceRecipe) {
            return new ItemStack(Material.FURNACE);
        } else if (recipe instanceof SmithingRecipe) {
            return new ItemStack(Material.SMITHING_TABLE);
        } else if (recipe instanceof SmokingRecipe) {
            return new ItemStack(Material.SMOKER);
        } else if (recipe instanceof BlastingRecipe) {
            return new ItemStack(Material.BLAST_FURNACE);
        } else if (recipe instanceof CampfireRecipe) {
            return new ItemStack(Material.CAMPFIRE);
        } else if (recipe instanceof StonecuttingRecipe) {
            return new ItemStack(Material.STONECUTTER);
        } else if (recipe instanceof MerchantRecipe) {
            return new ItemStack(Material.VILLAGER_SPAWN_EGG);
        } else {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cUnknown recipe type: §f" + recipe.getClass().getSimpleName());
                item.setItemMeta(meta);
            }

            return item;
        }
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
