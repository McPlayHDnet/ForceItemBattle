package forceitembattle.util;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ForceItemPlayer {

    @Setter
    private Player player;
    private List<ForceItem> foundItems;
    @Setter
    private Material currentMaterial;
    @Setter
    private int remainingJokers;
    @Setter
    private Integer currentScore;
    @Setter
    private Team currentTeam;
    @Setter
    private int backToBackStreak;

    public ForceItemPlayer(Player player, List<ForceItem> foundItems, Material currentMaterial, int remainingJokers, Integer currentScore) {
        this.player = player;
        this.foundItems = foundItems;
        this.currentMaterial = currentMaterial;
        this.remainingJokers = remainingJokers;
        this.currentScore = currentScore;
    }

    public Player player() {
        return player;
    }

    public List<ForceItem> foundItems() {
        return foundItems;
    }

    public void addFoundItemToList(ForceItem forceItem) {
        this.foundItems.add(forceItem);
    }

    public Material currentMaterial() {
        return currentMaterial;
    }

    public Material getCurrentMaterial() {
        if (ForceItemBattle.getInstance().getSettings().isSettingEnabled(GameSetting.TEAM)) {
            return currentTeam().getCurrentMaterial();
        }

        return currentMaterial();
    }

    public Material previousMaterial() {
        return this.foundItems.get(this.foundItems.size() - 1).material();
    }

    public int remainingJokers() {
        return remainingJokers;
    }

    public Integer currentScore() {
        return currentScore;
    }

    public Team currentTeam() {
        return currentTeam;
    }

    public int backToBackStreak() {
        return backToBackStreak;
    }

    private ArmorStand itemDisplay;

    public void createItemDisplay() {
        this.itemDisplay = player.getWorld().spawn(player.getLocation().add(0, 2, 0), ArmorStand.class);

        if (itemDisplay.getEquipment() != null) {
            itemDisplay.getEquipment().setHelmet(new ItemStack(currentMaterial()));
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
            equipment.setHelmet(new ItemStack(currentMaterial()));
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
