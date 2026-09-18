package forceitembattle.randomevents;

import forceitembattle.model.CustomMaterials;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Payouts shared by the events that hand out wheels. Both {@link ItemHunt} and {@link PointHunt}
 * settled on the same rule and had drifted into two identical private copies of it.
 */
final class EventRewards {

    private EventRewards() {
    }

    /**
     * Pays a player their wheels, dropping at their feet whatever the inventory could not take, so a
     * full inventory costs the winner nothing.
     */
    static void giveWheels(Player player, int amount) {
        ItemStack wheels = CustomMaterials.WHEEL_OF_FORTUNE.itemStack(amount);

        player.getInventory().addItem(wheels).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
