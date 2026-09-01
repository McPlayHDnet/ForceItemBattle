package forceitembattle.listener;

import forceitembattle.model.RoundPhase;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.GameSetting;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

@RequiredArgsConstructor
public class PvPListener implements Listener {

    /** Environmental burns that can be pinned on a nearby player, so they count as PvP. */
    private static final Set<EntityDamageEvent.DamageCause> FIRE_CAUSES = Set.of(
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.LAVA);

    private final RoundPhase roundPhase;
    private final GameSettings settings;

    @EventHandler
    public void onTntIgnited(EntitySpawnEvent e) {
        if (isPvpEnabled()) {
            return;
        }

        if (e.getEntityType() != EntityType.TNT) {
            return;
        }

        if (getPlayersNearby(e.getLocation().getBlock()) > 1) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTntMinecart(EntityExplodeEvent e) {
        if (isPvpEnabled()) {
            return;
        }

        if (e.getEntity().getType() == EntityType.TNT_MINECART) {
            e.setCancelled(true);
        }
    }

    public int getPlayersNearby(Block block) {
        return (int) block.getWorld().getNearbyEntities(block.getLocation(), 6, 6, 6).stream()
                .filter(Player.class::isInstance)
                .count();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!this.roundPhase.roundRunning()) {
            event.setCancelled(true);
        }

        if (isPvpEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }

        if (FIRE_CAUSES.contains(event.getCause())) {
            for (Entity nearby : event.getEntity().getNearbyEntities(6, 6, 6)) {
                if (!(nearby instanceof Player)) {
                    continue;
                }

                boolean isSameAsDamaged = nearby.getName().equalsIgnoreCase(event.getEntity().getName());
                if (!isSameAsDamaged) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (this.roundPhase.isPausedGame() && event.getTarget() instanceof Player) {
            event.setCancelled(true);
        }
    }

    private boolean isPvpEnabled() {
        return this.settings.isSettingEnabled(GameSetting.PVP);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvpDisabled(EntityDamageByEntityEvent event) {
        if (isPvpEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getDamager().getType() == EntityType.TNT_MINECART) {
            event.setCancelled(true);
            return;
        }

        if (!(getEntityOrigin(event.getDamager()) instanceof Player)) {
            return;
        }

        event.setCancelled(true);
    }

    private Object getEntityOrigin(Entity entity) {
        if (entity instanceof Projectile projectile) {
            return projectile.getShooter();
        }

        if (entity instanceof TNTPrimed tntPrimed) {
            return tntPrimed.getSource();
        }

        return entity;
    }
}
