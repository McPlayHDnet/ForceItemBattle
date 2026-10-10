package forceitembattle.randomevents;

import forceitembattle.model.CustomMaterials;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class EventRewards {

    private EventRewards() {
    }

    /** Drops whatever the inventory can't take, so a full inventory costs the winner nothing. */
    static void giveWheels(Player player, int amount) {
        ItemStack wheels = CustomMaterials.WHEEL_OF_FORTUNE.itemStack(amount);

        player.getInventory().addItem(wheels).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
