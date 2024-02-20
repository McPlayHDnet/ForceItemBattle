package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import org.apache.commons.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RecipeInventory extends InventoryBuilder {

    private static final int RESULT_SLOT = 25;
    private static final int STATION_SLOT = 23;
    private static final int WORKBENCH_FIRST_ITEM_SLOT = 10;
    private static final int SMITHING_FIRST_ITEM_SLOT = 19;
    private static final int OTHER_FIRST_ITEM_SLOT = 20;
    public static final int NEXT_RECIPE_ITEM_SLOT = 8;
    public static final int PREVIOUS_RECIPE_ITEM_SLOT = 0;

    /**
     * Slots that contain recipe items, the station and result items.
     */
    public static final List<Integer> SLOTS = List.of(
            10, 11, 12,
            19, 20, 21,
            28, 29, 30,
            STATION_SLOT, RESULT_SLOT
    );

    public RecipeInventory(ForceItemBattle forceItemBattle, RecipeViewer recipeViewer, Player player) {
        super(9 * 5, "§8● §3" +
                WordUtils.capitalize(recipeViewer.itemStack().getType().name().replace("_", " ").toLowerCase()) +
                " §8» §7" + (recipeViewer.currentRecipeIndex() + 1) + "§8/§7" + recipeViewer.pages()
        );

        for (int i = 0; i < this.getInventory().getSize(); i++) {
            if (!SLOTS.contains(i)) {
                this.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("§2").getItemStack());
            }
        }

        if (forceItemBattle.getRecipeManager().closeHandlers.containsKey(recipeViewer.uuid())) {
            forceItemBattle.getRecipeManager().handleRecipeClose(player);
        }

        forceItemBattle.getRecipeManager().ignoreCloseHandler.put(recipeViewer.uuid(), false);

        forceItemBattle.getRecipeManager().closeHandlers.put(recipeViewer.uuid(), () -> forceItemBattle.getRecipeManager().ignoreCloseHandler.remove(player.getUniqueId()));

        this.addUpdateHandler(() -> {
            this.setItem(PREVIOUS_RECIPE_ITEM_SLOT, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName("§4« §cPrevious Recipe").getItemStack(), inventoryClickEvent -> {
                int currentRecipeIndex = recipeViewer.currentRecipeIndex();

                if(currentRecipeIndex != 0) {
                    currentRecipeIndex--;

                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);

                    recipeViewer.setCurrentRecipeIndex(currentRecipeIndex);
                    if(Bukkit.getRecipesFor(recipeViewer.itemStack()).size() > 1) {
                        recipeViewer.setRecipe(Bukkit.getRecipesFor(recipeViewer.itemStack()).get(recipeViewer.currentRecipeIndex()));
                    } else {
                        recipeViewer.setRecipe(Bukkit.getRecipesFor(recipeViewer.itemStack()).get(0));
                    }
                    new RecipeInventory(forceItemBattle, recipeViewer, player).open(player);

                } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            });

            this.setItem(NEXT_RECIPE_ITEM_SLOT, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName("§2» §aNext Recipe").getItemStack(), inventoryClickEvent -> {
                int currentRecipeIndex = recipeViewer.currentRecipeIndex();

                if(currentRecipeIndex != (recipeViewer.pages() - 1)) {
                    currentRecipeIndex++;

                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);

                    recipeViewer.setCurrentRecipeIndex(currentRecipeIndex);
                    if(Bukkit.getRecipesFor(recipeViewer.itemStack()).size() > 1) {
                        recipeViewer.setRecipe(Bukkit.getRecipesFor(recipeViewer.itemStack()).get(recipeViewer.currentRecipeIndex()));
                    } else {
                        recipeViewer.setRecipe(Bukkit.getRecipesFor(recipeViewer.itemStack()).get(0));
                    }
                    new RecipeInventory(forceItemBattle, recipeViewer, player).open(player);

                } else player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
            });
        });


        List<ItemStack> ingredients = new ArrayList<>();

        if (recipeViewer.recipe() instanceof ShapedRecipe shaped) {
            String[] shape = shaped.getShape();

            int rowIndex = 0;
            for (String row : shape) {
                int charIndex = 0;
                for (char c : row.toCharArray()) {
                    int slot = WORKBENCH_FIRST_ITEM_SLOT + rowIndex * 9 + charIndex;

                    RecipeChoice choice = shaped.getChoiceMap().get(c);
                    if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {

                        List<String> lore = new ArrayList<>();
                        ItemBuilder itemBuilder = new ItemBuilder(materialChoice.getChoices().get(0));

                        materialChoice.getChoices().subList(1, materialChoice.getChoices().size()).forEach(choices -> {
                            lore.add(" §8» §3" + WordUtils.capitalize(choices.name().replace("_", " ").toLowerCase()));

                            if(choices.name().contains("_PLANKS")) {
                                lore.clear();
                                lore.add(" §8» §3any wooden plank");
                            }

                            if(recipeViewer.itemStack().getType() == Material.SMOKER || recipeViewer.itemStack().getType() == Material.CAMPFIRE || recipeViewer.itemStack().getType() == Material.SOUL_CAMPFIRE) {
                                lore.clear();
                                lore.add(" §8» §3any wooden log/wood (and stripped variants)");
                            }
                        });

                        itemBuilder.setLore(lore);
                        lore.clear();

                        this.setItem(slot, itemBuilder.getItemStack());

                    } else if (choice != null) {
                        this.setItem(slot, new ItemStack(choice.getItemStack()));
                    }
                    charIndex++;
                }
                rowIndex++;
            }

        }
        if (recipeViewer.recipe() instanceof ShapelessRecipe shapeless) {
            for (RecipeChoice recipeChoice : shapeless.getChoiceList()) {
                if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    System.out.println(materialChoice.getChoices().size() + " shapeless");
                    ItemStack fixed = new ItemStack(materialChoice.getChoices().get(0));
                    ingredients.add(fixed);
                } else if (recipeChoice != null) {
                    ingredients.add(new ItemStack(recipeChoice.getItemStack()));
                }
            }

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                this.setItem(convertItemIndexToInventorySlot(WORKBENCH_FIRST_ITEM_SLOT, index), ingredient);
                index++;
            }
        }
        if (recipeViewer.recipe() instanceof CookingRecipe<?> furnace) {
            if (furnace.getInputChoice() instanceof RecipeChoice.MaterialChoice materialChoice) {
                ItemStack fixed = new ItemStack(materialChoice.getChoices().get(0));

                this.setItem(OTHER_FIRST_ITEM_SLOT, fixed);
            }

        }
        if (recipeViewer.recipe() instanceof SmithingTrimRecipe smithing) {
            ingredients.add(smithing.getBase().getItemStack());
            ingredients.add(smithing.getTemplate().getItemStack());
            ingredients.add(smithing.getAddition().getItemStack());

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                this.setItem(OTHER_FIRST_ITEM_SLOT + index, ingredient);
                index++;
            }

        } else if (recipeViewer.recipe() instanceof SmithingTransformRecipe smithing) {
            ingredients.add(smithing.getBase().getItemStack());
            ingredients.add(smithing.getTemplate().getItemStack());
            ingredients.add(smithing.getAddition().getItemStack());

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                this.setItem(SMITHING_FIRST_ITEM_SLOT + index, ingredient);
                index++;
            }

        } else if (recipeViewer.recipe() instanceof SmithingRecipe smithing) {
            // Unknown smithing recipe?
            ingredients.add(smithing.getAddition().getItemStack());
            ingredients.add(smithing.getBase().getItemStack());

            int index = 0;
            for (ItemStack ingredient : ingredients) {
                this.setItem(SMITHING_FIRST_ITEM_SLOT + index, ingredient);
                index++;
            }

        }
        if (recipeViewer.recipe() instanceof MerchantRecipe merchant) {
            ItemStack fixed = new ItemStack(merchant.getResult().getType(), 1, (byte) 0);
            ingredients.add(fixed);

            this.setItem(OTHER_FIRST_ITEM_SLOT, fixed);

        }
        if (recipeViewer.recipe() instanceof StonecuttingRecipe stonecutting) {
            ItemStack fixed = new ItemStack(stonecutting.getInput());
            ingredients.add(fixed);

            this.setItem(OTHER_FIRST_ITEM_SLOT, fixed);
        }

        this.setItem(RESULT_SLOT, recipeViewer.recipe().getResult());
        this.setItem(STATION_SLOT, getStationItem(recipeViewer.recipe()));


        this.addClickHandler(inventoryClickEvent -> {
            if (inventoryClickEvent.getClickedInventory() == null) {
                return;
            }

            inventoryClickEvent.setCancelled(true);

            if (!inventoryClickEvent.getClickedInventory().equals(inventoryClickEvent.getView().getTopInventory())) {
                return;
            }

            if (!RecipeInventory.SLOTS.contains(inventoryClickEvent.getSlot())) {
                return;
            }

            if (inventoryClickEvent.getClick().isShiftClick()) {
                ItemStack itemStack = inventoryClickEvent.getCurrentItem();
                if (itemStack == null) {
                    return;
                }
                if(Bukkit.getRecipesFor(itemStack).isEmpty()) {
                    player.sendMessage("§cThere is no recipe for this item. Just find it lol");
                    return;
                }
                recipeViewer.setCurrentRecipeIndex(0);
                recipeViewer.setItemStack(itemStack);
                if(Bukkit.getRecipesFor(recipeViewer.itemStack()).size() > 1) {
                    recipeViewer.setRecipe(Bukkit.getRecipesFor(recipeViewer.itemStack()).get(recipeViewer.currentRecipeIndex()));
                } else {
                    recipeViewer.setRecipe(Bukkit.getRecipesFor(recipeViewer.itemStack()).get(0));
                }
                new RecipeInventory(forceItemBattle, recipeViewer, player).open(player);

            } else {
                player.sendMessage("§cSneak click to show recipe for this item!");
            }
        });

        this.addCloseHandler(inventoryCloseEvent -> {
            if (forceItemBattle.getRecipeManager().isShowingRecipe(player) && !forceItemBattle.getRecipeManager().ignoreInventoryClosed(player)) {
                inventoryCloseEvent.getInventory().clear();
                forceItemBattle.getRecipeManager().handleRecipeClose(player);
            }
        });
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

    private int convertItemIndexToInventorySlot(int firstItemSlot, int itemIndex) {
        return firstItemSlot + itemIndex % 3 + 9 * (itemIndex / 3);
    }
}
