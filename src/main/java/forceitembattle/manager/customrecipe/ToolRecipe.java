package forceitembattle.manager.customrecipe;

import forceitembattle.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public class ToolRecipe extends ShapelessRecipe {

    public ToolRecipe(NamespacedKey key, ItemStack result) {
        super(key, result);
    }

    private final List<String> interactionLore = new ArrayList<>();

    private ItemStack stationDisplay;

    public void addInteractionLore(String... lore) {
        for (String line : lore) {
            interactionLore.add(
                    ChatColor.translateAlternateColorCodes('&', line)
            );
        }
    }

    public void setStationDisplay(ItemStack stationDisplay) {
        this.stationDisplay = stationDisplay;
    }

    public ItemStack getStationDisplay() {
        ItemBuilder displayItem = new ItemBuilder(Material.STONE_PICKAXE);

        if (stationDisplay != null) {
            displayItem = new ItemBuilder(stationDisplay.clone());
        }

        return displayItem
                .addEnchantment(Enchantment.LUCK, 1)
                .addItemFlag(ItemFlag.HIDE_ENCHANTS)
                .setDisplayName("&fHow to get item:")
                .setLore(getInteractionLore())
                .getItemStack();
    }

    public List<String> getInteractionLore() {
        return interactionLore;
    }

}
