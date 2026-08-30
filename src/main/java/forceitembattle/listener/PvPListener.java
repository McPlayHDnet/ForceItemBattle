package forceitembattle.listener;

import forceitembattle.model.RoundPhase;
import forceitembattle.settings.GameSettings;
import forceitembattle.settings.GameSetting;
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
        int totalPlayers = 0;
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 6, 6, 6)) {
            if (entity instanceof Player) {
                totalPlayers++;
            }
        }
        return totalPlayers;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!this.roundPhase.roundRunning()) {
            event.setCancelled(true);
        }

        if (isPvpEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
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
        if (entity instanceof Projectile) {
            return ((Projectile) entity).getShooter();
        }

        if (entity instanceof TNTPrimed) {
            return ((TNTPrimed) entity).getSource();
        }

        return entity;
    }
}
