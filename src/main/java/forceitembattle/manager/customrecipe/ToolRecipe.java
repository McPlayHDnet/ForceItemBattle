package forceitembattle.manager.customrecipe;

import forceitembattle.gui.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

public class ToolRecipe extends ShapelessRecipe {

    private final ItemStack stationDisplay;
    private final List<String> interactionLore;

    public ToolRecipe(NamespacedKey key, ItemStack result, Material station, String... interactionLore) {
        super(key, result);
        this.stationDisplay = new ItemStack(station);
        this.interactionLore = List.of(interactionLore);
    }

    public ItemStack getStationDisplay() {
        return new ItemBuilder(this.stationDisplay.clone())
                .addEnchantment(Enchantment.FORTUNE, 1)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .setDisplayNameLegacy("&fHow to get item:")
                .setLoreLegacy(this.interactionLore)
                .getItemStack();
    }
}
