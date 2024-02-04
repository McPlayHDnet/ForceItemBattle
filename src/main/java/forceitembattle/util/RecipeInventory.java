package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

@SuppressWarnings("deprecation")
public class RecipeInventory {

    private ForceItemBattle forceItemBattle;

    public RecipeInventory(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
    }


    /**
     * Player UUD -> remove on close.
     * If value is true, ignore close inventory
     */
    private final HashMap<UUID, Boolean> ignoreCloseHandler = new HashMap<>();

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

    /**
     * Player UUID -> handle inventory being closed by the player
     */
    private final HashMap<UUID, Runnable> closeHandlers = new HashMap<>();

    public void showRecipe(Player player, ItemStack item) {
        if (Bukkit.getRecipesFor(item).isEmpty()) {
            player.sendMessage("§cThere is no recipe for this item. Just find it lol");
            return;
        }

        List<Inventory> inventories = createInventories(player, item);

        if (inventories.isEmpty()) {
            TextComponent message = new TextComponent("§cThere were recipes for this item that we cannot display, for some reason! ");
            TextComponent infoWiki = new TextComponent("§fTry /infowiki");
            infoWiki.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/infowiki"));

            player.spigot().sendMessage(message, infoWiki);
            return;
        }

        if (closeHandlers.containsKey(player.getUniqueId())) {
            handleRecipeClose(player);
        }

        ignoreCloseHandler.put(player.getUniqueId(), false);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.forceItemBattle, new Runnable() {

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

    private List<Inventory> createInventories(Player player, ItemStack item) {
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

    /**
     * Slots that contain recipe items, the station and result items.
     */
    public static final List<Integer> SLOTS = List.of(
            10, 11, 12,
            19, 20, 21,
            28, 29, 30,
            STATION_SLOT, RESULT_SLOT
    );

    private Inventory createFancyRecipeInventory(ItemStack item, Recipe recipe) {
        String itemName = WordUtils.capitalize(item.getType().name().replace("_", " ").toLowerCase());
        Inventory inventory = Bukkit.createInventory(null, 5 * 9, "§8● §6" + itemName);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (!SLOTS.contains(i)) {
                inventory.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§2").getItemStack());
            }
        }

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
                    ItemStack fixed = new ItemStack(materialChoice.getChoices().get(0), 1);
                    ingredients.add(fixed);
                } else if (recipeChoice != null) {
                    ingredients.add(new ItemStack(recipeChoice.getItemStack()));
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

    private ItemStack getStationItem(Recipe recipe) {
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

    private Inventory createRecipeInventory(ItemStack item, Recipe recipe) {
        Inventory inventory;
        List<ItemStack> ingredients = new ArrayList<>();

        if (recipe instanceof ShapedRecipe shaped) {
            inventory = Bukkit.createInventory(null, InventoryType.WORKBENCH);

            CraftingInventory craftingInventory = (CraftingInventory) inventory;
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

        } else if (recipe instanceof ShapelessRecipe shapeless) {
            inventory = Bukkit.createInventory(null, InventoryType.WORKBENCH);

            CraftingInventory craftingInventory = (CraftingInventory) inventory;
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

        } else if (recipe instanceof FurnaceRecipe furnace) {
            inventory = Bukkit.createInventory(null, InventoryType.FURNACE);

            FurnaceInventory furnaceInventory = (FurnaceInventory) inventory;
            ItemStack fixed = new ItemStack(furnace.getInput().getType(), 1, (byte) 0);
            ingredients.add(fixed);

            furnaceInventory.setSmelting(ingredients.get(0));
            furnaceInventory.setResult(item);

        } else if (recipe instanceof SmithingRecipe smithing) {
            inventory = Bukkit.createInventory(null, InventoryType.SMITHING);

            SmithingInventory smithingInventory = (SmithingInventory) inventory;
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

    private int convertItemIndexToInventorySlot(int firstItemSlot, int itemIndex) {
        return firstItemSlot + itemIndex % 3 + 9 * (itemIndex / 3);
    }
}
