package forceitembattle.listener;

import forceitembattle.manager.Gamemanager;
import forceitembattle.model.RoundPhase;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.GameSetting;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@RequiredArgsConstructor
public class GameRulesListener implements Listener {
    private final RoundPhase roundPhase;
    private final GameSettings settings;
    @EventHandler
    public void onOffHand(PlayerSwapHandItemsEvent event) {
        if (Gamemanager.isBackpack(event.getMainHandItem()) ||
                Gamemanager.isBackpack(event.getOffHandItem())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (Gamemanager.isJoker(event.getItemDrop().getItemStack())
                || Gamemanager.isBackpack(event.getItemDrop().getItemStack())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand armorStand && armorStand.isInvisible()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        // Countdown counts as lobby here: no one should lose hunger before the game actually runs.
        if (this.roundPhase.isPreGame() || this.roundPhase.isStarting()) {
            event.setCancelled(true);
            return;
        }

        if (!this.settings.isSettingEnabled(GameSetting.FOOD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (this.roundPhase.roundRunning()) {
            return;
        }
        if (event.getTarget() == null) {
            return;
        }
        if (event.getTarget().getType() != EntityType.PLAYER) {
            return;
        }
        event.setTarget(null);
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        if (this.roundPhase.isPausedGame()) {
            Location from = playerMoveEvent.getFrom();
            Location to = playerMoveEvent.getTo();

            if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {

                double newX = from.getBlockX() + 0.5;
                double newZ = from.getBlockZ() + 0.5;
                double newYaw = playerMoveEvent.getPlayer().getLocation().getYaw();

                Location newLocation = new Location(from.getWorld(), newX, from.getY(), newZ, (float) newYaw, from.getPitch());
                playerMoveEvent.setTo(newLocation);
            }
        }
    }
}
