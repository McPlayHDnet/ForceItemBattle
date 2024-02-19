package forceitembattle.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.*;

import java.util.UUID;

public class RecipeViewer {

    private UUID uuid;
    private ItemStack itemStack;
    private Recipe recipe;
    private int currentRecipeIndex;
    private int currentPage;
    private int currentChoice;
    private int pages;

    public RecipeViewer() {

    }

    public void createPages() {
        if (this.recipe() instanceof ShapedRecipe shaped) {
            String[] shape = shaped.getShape();

            for (String row : shape) {
                for (char c : row.toCharArray()) {

                    RecipeChoice choice = shaped.getChoiceMap().get(c);
                    if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                        this.setPages(materialChoice.getChoices().size());
                    }
                }
            }

        }
        if (this.recipe() instanceof ShapelessRecipe shapeless) {
            for (RecipeChoice recipeChoice : shapeless.getChoiceList()) {
                if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                    this.setPages(materialChoice.getChoices().size());
                }
            }
        }
        if (this.recipe() instanceof CookingRecipe<?> furnace) {

            if (furnace.getInputChoice() instanceof RecipeChoice.MaterialChoice materialChoice) {
                this.setPages(materialChoice.getChoices().size());
            }

        }
        if (this.recipe() instanceof SmithingTrimRecipe smithing) {
            if (smithing.getTemplate() instanceof RecipeChoice.MaterialChoice materialChoice) {
                this.setPages(materialChoice.getChoices().size());
            }

        }
        if (this.recipe() instanceof SmithingTransformRecipe smithing) {
            if (smithing.getTemplate() instanceof RecipeChoice.MaterialChoice materialChoice) {
                this.setPages(materialChoice.getChoices().size());
            }

        }
        if (this.recipe() instanceof SmithingRecipe smithing) {
            if (smithing.getAddition() instanceof RecipeChoice.MaterialChoice materialChoice) {
                this.setPages(materialChoice.getChoices().size());
            }

        }
        if (this.recipe() instanceof StonecuttingRecipe stonecutting) {
            if (stonecutting.getInputChoice() instanceof RecipeChoice.MaterialChoice materialChoice) {
                this.setPages(materialChoice.getChoices().size());
            }
        }

        this.setPages(this.pages() + (this.recipes() - 1));
    }

    private int recipes() {
        return Bukkit.getRecipesFor(this.itemStack()).size();
    }

    public UUID uuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public Recipe recipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public int currentRecipeIndex() {
        return currentRecipeIndex;
    }

    public void setCurrentRecipeIndex(int recipeIndex) {
        this.currentRecipeIndex = recipeIndex;
    }

    public int currentPage() {
        return (this.recipes() > 1 ? currentPage - this.recipes() + 1 : currentPage);
    }

    public int currentPageContainer() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int currentChoice() {
        return currentChoice;
    }

    public void setCurrentChoice(int currentChoice) {
        this.currentChoice = currentChoice;
    }

    public int pages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
