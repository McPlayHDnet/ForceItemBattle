package forceitembattle.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public final class GuiItems {

    private GuiItems() {
    }

    /** Neutral filler for the body of a menu. */
    public static ItemStack filler() {
        return blankPane(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** Border for settings-style menus. */
    public static ItemStack border() {
        return blankPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    }

    /** Border for the vault and achievement menus. */
    public static ItemStack accentBorder() {
        return blankPane(Material.CYAN_STAINED_GLASS_PANE);
    }

    public static ItemStack nextPage() {
        return next("Next Page");
    }

    public static ItemStack previousPage() {
        return previous("Previous Page");
    }

    /** Forward button, e.g. {@code next("Next Recipe")}. */
    public static ItemStack next(String label) {
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setDisplayName("<dark_green>» <green>" + label)
                .addItemFlags(ItemFlag.values())
                .getItemStack();
    }

    /** Back button, e.g. {@code previous("Previous Recipe")}. */
    public static ItemStack previous(String label) {
        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setDisplayName("<dark_red>« <red>" + label)
                .addItemFlags(ItemFlag.values())
                .getItemStack();
    }

    public static ItemStack noItemsFound() {
        return new ItemBuilder(Material.BARRIER)
                .setDisplayName("<red>No Items found")
                .getItemStack();
    }

    private static ItemStack blankPane(Material material) {
        return new ItemBuilder(material)
                .setDisplayName("")
                .addItemFlags(ItemFlag.values())
                .getItemStack();
    }
}
