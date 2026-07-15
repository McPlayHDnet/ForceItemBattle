package forceitembattle.util;

import forceitembattle.manager.Gamemanager;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public final class InventorySearch {

    private InventorySearch() {
    }

    /**
     * Whether the inventory holds the material anywhere, including inside shulker
     * boxes and bundles.
     */
    public static boolean contains(@Nullable Inventory inventory, Material targetMaterial) {
        if (inventory == null) {
            return false;
        }

        for (ItemStack item : inventory.getContents()) {
            if (containsMaterial(item, targetMaterial)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds every distinct material in the inventory to {@code into}, including the
     * contents of shulker boxes and bundles. Accumulates across calls, so several
     * inventories (players, team backpack) can be folded into one set.
     */
    public static void collectUniqueMaterials(@Nullable Inventory inventory, Set<Material> into) {
        if (inventory == null) {
            return;
        }

        for (ItemStack item : inventory.getContents()) {
            if (isPluginItem(item)) {
                continue;
            }

            Material type = item.getType();
            into.add(type);

            if (Tag.SHULKER_BOXES.isTagged(type)) {
                collectFromShulkerBox(item, into);
            }

            if (Tag.ITEMS_BUNDLES.isTagged(type)) {
                collectFromBundle(item, into);
            }
        }
    }

    private static boolean containsMaterial(@Nullable ItemStack item, Material targetMaterial) {
        if (isPluginItem(item)) {
            return false;
        }

        if (item.getType() == targetMaterial) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (meta instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
                if (containsMaterial(shulkerItem, targetMaterial)) {
                    return true;
                }
            }
        }

        if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
            for (ItemStack bundleItem : bundleMeta.getItems()) {
                if (containsMaterial(bundleItem, targetMaterial)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void collectFromShulkerBox(ItemStack shulkerBox, Set<Material> into) {
        ItemMeta meta = shulkerBox.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return;
        }
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox box)) {
            return;
        }

        for (ItemStack item : box.getInventory().getContents()) {
            if (isPluginItem(item)) {
                continue;
            }

            Material type = item.getType();
            into.add(type);

            if (Tag.ITEMS_BUNDLES.isTagged(type)) {
                collectFromBundle(item, into);
            }
        }
    }

    private static void collectFromBundle(ItemStack bundle, Set<Material> into) {
        ItemMeta meta = bundle.getItemMeta();
        if (!(meta instanceof BundleMeta bundleMeta)) {
            return;
        }
        if (!bundleMeta.hasItems()) {
            return;
        }

        for (ItemStack item : bundleMeta.getItems()) {
            if (isPluginItem(item)) {
                continue;
            }

            into.add(item.getType());
        }
    }

    /** Null, or one of the plugin's own tool items — never counts as a found item. */
    private static boolean isPluginItem(@Nullable ItemStack item) {
        return item == null || Gamemanager.isJoker(item) || Gamemanager.isBackpack(item);
    }
}
