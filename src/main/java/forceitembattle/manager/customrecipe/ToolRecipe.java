package forceitembattle.manager.customrecipe;

import forceitembattle.gui.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

public class ToolRecipe extends ShapelessRecipe {

    private final List<String> interactionLore = new ArrayList<>();
    private ItemStack stationDisplay;

    public ToolRecipe(NamespacedKey key, ItemStack result) {
        super(key, result);
    }

    public void addInteractionLore(String... lore) {
        interactionLore.addAll(List.of(lore));
    }

    public ItemStack getStationDisplay() {
        ItemStack base = stationDisplay != null ? stationDisplay.clone() : new ItemStack(Material.STONE_PICKAXE);

        return new ItemBuilder(base)
                .addEnchantment(Enchantment.FORTUNE, 1)
                .addItemFlag(ItemFlag.HIDE_ENCHANTS)
                .setDisplayNameLegacy("&fHow to get item:")
                .setLoreLegacy(getInteractionLore())
                .getItemStack();
    }

    public void setStationDisplay(ItemStack stationDisplay) {
        this.stationDisplay = stationDisplay;
    }

    public List<String> getInteractionLore() {
        return interactionLore;
    }

}
