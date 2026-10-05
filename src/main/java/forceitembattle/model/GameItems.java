package forceitembattle.model;

import forceitembattle.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Builder and recogniser stay together: death drops filter on the recognisers, so a mismatch loses jokers and backpacks. */
public final class GameItems {

    public static final NamespacedKey BACKPACK_KEY = new NamespacedKey("fib", "backpack");

    private static final Material JOKER_MATERIAL = Material.BARRIER;

    private GameItems() {
    }

    public static Material jokerMaterial() {
        return JOKER_MATERIAL;
    }

    public static ItemStack jokers(int amount) {
        return new ItemBuilder(JOKER_MATERIAL)
                .setAmount(amount)
                .setDisplayName("<dark_gray>» <dark_purple>Joker")
                .getItemStack();
    }

    public static ItemStack backpack(ForceItemPlayer forceItemPlayer) {
        Material bundle = Material.BUNDLE;
        if (forceItemPlayer.isInTeam()) {
            bundle = Material.getMaterial(forceItemPlayer.currentTeam().getColor().name() + "_BUNDLE");
        }

        ItemStack itemStack = new ItemBuilder(bundle)
                .setDisplayName("<dark_gray>» <yellow>Backpack")
                .getItemStack();

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(BACKPACK_KEY, PersistentDataType.BOOLEAN, Boolean.TRUE);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private static boolean isJoker(Material material) {
        return material == JOKER_MATERIAL;
    }

    public static boolean isJoker(ItemStack itemStack) {
        return isJoker(itemStack.getType());
    }

    public static boolean isBackpack(ItemStack itemStack) {
        if (!itemStack.getType().name().contains("BUNDLE")) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!itemMeta.getPersistentDataContainer().has(BACKPACK_KEY)) {
            return false;
        }

        return Boolean.TRUE.equals(itemMeta.getPersistentDataContainer()
                .get(BACKPACK_KEY, PersistentDataType.BOOLEAN));
    }
}
