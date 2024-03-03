package forceitembattle.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ForceItemPlayer {

    @Getter
    @Setter
    private Player player;
    @Getter
    private final List<ForceItem> foundItems;
    @Getter
    @Setter
    private Material currentMaterial;
    @Getter
    @Setter
    private int remainingJokers;
    @Getter
    @Setter
    private Integer currentScore;

    public ForceItemPlayer(Player player, List<ForceItem> foundItems, Material currentMaterial, int remainingJokers, Integer currentScore) {
        this.player = player;
        this.foundItems = foundItems;
        this.currentMaterial = currentMaterial;
        this.remainingJokers = remainingJokers;
        this.currentScore = currentScore;
    }

    public void addFoundItemToList(ForceItem forceItem) {
        this.foundItems.add(forceItem);
    }

    public Material getPreviousMaterial() {
        return this.foundItems.get(this.foundItems.size() - 1).material();
    }

    private ArmorStand itemDisplay;

    public void createItemDisplay() {
        this.itemDisplay = player.getWorld().spawn(player.getLocation().add(0, 2, 0), ArmorStand.class);

        if (itemDisplay.getEquipment() != null) {
            itemDisplay.getEquipment().setHelmet(new ItemStack(getCurrentMaterial()));
            itemDisplay.setInvisible(true);
            itemDisplay.setInvulnerable(true);
            itemDisplay.setGravity(false);
        }

        player.addPassenger(itemDisplay);
    }

    public void removeItemDisplay() {
        if (itemDisplay == null) {
            return;
        }

        itemDisplay.remove();
    }

    public void updateItemDisplay() {
        ensureItemDisplay();

        EntityEquipment equipment = itemDisplay.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new ItemStack(getCurrentMaterial()));
        }
    }

    private void ensureItemDisplay() {
        if (itemDisplay == null) {
            createItemDisplay();
            return;
        }

        if (!itemDisplay.isValid()) {
            itemDisplay.remove();
            createItemDisplay();
            return;
        }

        if (!player.getPassengers().contains(itemDisplay)) {
            player.addPassenger(itemDisplay);
        }
    }
}
