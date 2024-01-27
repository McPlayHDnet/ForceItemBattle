package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ItemsInventory;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CommandInfo implements CommandExecutor {

    public static HashMap<UUID, Boolean> crafting = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if(ForceItemBattle.getTimer().isRunning()) {
            ItemStack itemStack = new ItemStack(ForceItemBattle.getGamemanager().getCurrentMaterial(player));
            if(!Bukkit.getRecipesFor(itemStack).isEmpty()) {

                CraftingInventory craftingInventory = null;
                FurnaceInventory furnaceInventory = null;
                SmithingInventory smithingInventory = null;

                List<ItemStack> ingredients = new ArrayList<>();
                for (Recipe recipe : Bukkit.getServer().getRecipesFor(itemStack)) {
                    if (recipe instanceof ShapedRecipe) {
                        InventoryView inventoryView = player.openWorkbench(null, true);
                        inventoryView.setTitle(WordUtils.capitalize(itemStack.getType().name().replace("_", " ").toLowerCase()));

                        craftingInventory = (CraftingInventory) inventoryView.getTopInventory();
                        ShapedRecipe shaped = (ShapedRecipe) recipe;
                        for(RecipeChoice recipeChoice : shaped.getChoiceMap().values()) {
                            if(recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                                materialChoice.getChoices().forEach(material -> {
                                    ItemStack fixed = new ItemStack(material, 1, (byte) 0);
                                    ingredients.add(fixed);
                                });

                            }
                        }
                    } else if (recipe instanceof ShapelessRecipe) {
                        InventoryView inventoryView = player.openWorkbench(null, true);
                        inventoryView.setTitle(WordUtils.capitalize(itemStack.getType().name().replace("_", " ").toLowerCase()));

                        craftingInventory = (CraftingInventory) inventoryView.getTopInventory();
                        ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
                        for(RecipeChoice recipeChoice : shapeless.getChoiceList()) {
                            if(recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                                materialChoice.getChoices().forEach(material -> {
                                    ItemStack fixed = new ItemStack(material, 1, (byte) 0);
                                    ingredients.add(fixed);
                                });

                            }
                        }

                    } else if (recipe instanceof FurnaceRecipe) {
                        InventoryView inventoryView = player.openInventory(Bukkit.createInventory(null, InventoryType.FURNACE));
                        inventoryView.setTitle(WordUtils.capitalize(itemStack.getType().name().replace("_", " ").toLowerCase()));

                        furnaceInventory = (FurnaceInventory) inventoryView.getTopInventory();
                        FurnaceRecipe furnace = (FurnaceRecipe) recipe;
                        ItemStack fixed = new ItemStack(furnace.getInput().getType(), 1, (byte) 0);
                        ingredients.add(fixed);

                    } else if (recipe instanceof SmithingRecipe) {
                        InventoryView inventoryView = player.openInventory(Bukkit.createInventory(null, InventoryType.SMITHING));
                        inventoryView.setTitle(WordUtils.capitalize(itemStack.getType().name().replace("_", " ").toLowerCase()));

                        smithingInventory = (SmithingInventory) inventoryView.getTopInventory();
                        SmithingRecipe smithing = (SmithingRecipe) recipe;
                        ingredients.add(smithing.getAddition().getItemStack());
                        ingredients.add(smithing.getBase().getItemStack());
                    }
                }

                if(furnaceInventory != null) {
                    furnaceInventory.setSmelting(ingredients.get(0));
                    furnaceInventory.setResult(itemStack);

                } else if(craftingInventory != null) {
                    craftingInventory.setMatrix(ingredients.toArray(new ItemStack[0]));
                    craftingInventory.setResult(itemStack);
                }

                player.updateInventory();

                crafting.put(player.getUniqueId(), true);
            } else {
                player.sendMessage("§cThere is no recipe for this item. Just find it");
            }

        }

        return false;
    }
}
