package forceitembattle.manager.customrecipe;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public class ToolRecipe extends ShapelessRecipe {

    public ToolRecipe(NamespacedKey key, ItemStack result) {
        super(key, result);
    }

    private final List<String> interactionLore = new ArrayList<>();

    public void addInteractionLore(String... lore) {
        for (String line : lore) {
            interactionLore.add(
                    ChatColor.translateAlternateColorCodes('&', line)
            );
        }
    }

    public List<String> getInteractionLore() {
        return interactionLore;
    }

}
