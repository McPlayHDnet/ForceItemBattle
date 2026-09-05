package forceitembattle.model;

import forceitembattle.gui.ItemBuilder;
import forceitembattle.manager.Gamemanager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * The two items the game hands out — the joker stack and the backpack — and the questions asked
 * about them afterwards.
 *
 * <p>Builder and recogniser belong together, and the pairing is load-bearing rather than tidy: on
 * death {@code PlayerLifecycleListener} filters the drops with {@code removeIf(GameItems::isJoker)}
 * and {@code removeIf(GameItems::isBackpack)}, so a recogniser that stops matching what the builder
 * produces means players drop — and permanently lose — their jokers and backpack when they die.
 * {@code GameItemsTest} is the pin for exactly that.
 *
 * <p>These lived on {@code Gamemanager} as statics, which is why eight modules that have no interest
 * in the round loop — three listeners, {@code BackpackManager}, {@code PlayerOutfitter},
 * {@code InventorySearch} and two more — imported the round orchestrator to ask "is this a joker".
 * Nothing here knows what a round is.
 */
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
