package forceitembattle.manager.customrecipe;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class RecipeBuilder <T extends Recipe>{

    private final BiFunction<NamespacedKey, ItemStack, T> recipeConstructor;
    private final List<Consumer<T>> operations = new ArrayList<>();

    public RecipeBuilder(BiFunction<NamespacedKey, ItemStack, T> constructor) {
        this.recipeConstructor = constructor;
    }

    public static <T extends Recipe> RecipeBuilder<T> newBuilder(BiFunction<NamespacedKey, ItemStack, T> constructor) {
        return new RecipeBuilder<>(constructor);
    }

    public RecipeBuilder<T> apply(Consumer<T> operation) {
        this.operations.add(operation);
        return this;
    }

    public T build(String namespacedKey, ItemStack result) {
        NamespacedKey key = NamespacedKey.fromString(namespacedKey);
        if (key == null) {
            throw new IllegalArgumentException("Invalid namespaced key: " + namespacedKey);
        }

        T recipe = this.recipeConstructor.apply(key, result);
        for (Consumer<T> operation : this.operations) {
            operation.accept(recipe);
        }

        return recipe;
    }
}
