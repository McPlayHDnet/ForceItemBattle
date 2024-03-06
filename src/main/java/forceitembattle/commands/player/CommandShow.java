package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CommandShow extends CustomCommand {

    public CommandShow() {
        super("show");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage("§cThe game has not started yet!");
            return;
        }

        if (!this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
            player.sendMessage("§cYou are not playing.");
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        ItemStack item = new ItemStack(forceItemPlayer.currentMaterial());

        spawnTemporaryArmorStand(player, item, 3);
    }

    private void spawnTemporaryArmorStand(Player player, ItemStack head, long durationSeconds) {
        Location standLocation = player.getEyeLocation();
        Vector direction = standLocation.getDirection().setY(0).normalize();

        standLocation.setDirection(
                standLocation.getDirection().multiply(-1) // Invert direction before spawning
        );

        // This should move stand away from player parallel to the eye direction.
        standLocation.add(direction);
        // Make armorstand's head appear on the eye level.
        standLocation.subtract(0, 1.75, 0);

        ArmorStand armorStand = player.getWorld().spawn(standLocation, ArmorStand.class);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true);

        armorStand.getEquipment().setHelmet(head);

        Scheduler.runLaterSync(armorStand::remove, durationSeconds * 20);
    }
}
