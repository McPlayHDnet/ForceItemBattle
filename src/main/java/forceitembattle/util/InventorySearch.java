package forceitembattle.util;

import forceitembattle.model.GameItems;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.jetbrains.annotations.Nullable;

public final class InventorySearch {

    private InventorySearch() {
    }

    /** Includes the contents of shulker boxes and bundles. */
    public static boolean contains(@Nullable Inventory inventory, Material targetMaterial) {
        return inventory != null && items(inventory.getContents()).anyMatch(item -> item.getType() == targetMaterial);
    }

    /** Includes shulker and bundle contents, and accumulates across calls. */
    public static void collectUniqueMaterials(@Nullable Inventory inventory, Set<Material> into) {
        if (inventory != null) {
            items(inventory.getContents()).forEach(item -> into.add(item.getType()));
        }
    }

    /** Every stack, containers' contents included; the plugin's own items never count. */
    private static Stream<ItemStack> items(ItemStack[] stacks) {
        return Arrays.stream(stacks)
                .filter(item -> item != null && !GameItems.isJoker(item) && !GameItems.isBackpack(item))
                .flatMap(item -> Stream.concat(Stream.of(item), items(contentsOf(item))));
    }

    /** Tag first, so item meta is only read for actual containers. */
    private static ItemStack[] contentsOf(ItemStack item) {
        Material type = item.getType();
        if (Tag.SHULKER_BOXES.isTagged(type) && item.getItemMeta() instanceof BlockStateMeta meta
                && meta.getBlockState() instanceof ShulkerBox box) {
            return box.getInventory().getContents();
        }
        if (Tag.ITEMS_BUNDLES.isTagged(type) && item.getItemMeta() instanceof BundleMeta meta) {
            return meta.getItems().toArray(ItemStack[]::new);
        }
        return new ItemStack[0];
    }
}
