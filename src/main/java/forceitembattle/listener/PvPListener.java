package forceitembattle.listener;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class PvPListener implements Listener {

    private final ForceItemBattle plugin;

    public PvPListener(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onTntIgnited(EntitySpawnEvent e) {
        if (isPvpEnabled()) {
            return;
        }

        if (e.getEntityType() != EntityType.PRIMED_TNT) {
            return;
        }

        // Disable spawning ignited TNT when there's more than 2 players nearby
        if (getPlayersNearby(e.getLocation().getBlock()) > 1) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTntMinecart(EntityExplodeEvent e) {
        if (isPvpEnabled()) {
            return;
        }

        if (e.getEntity().getType() == EntityType.MINECART_TNT) {
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
        if (!this.plugin.getGamemanager().isMidGame()) {
            event.setCancelled(true);
        }

        if (isPvpEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }

        // Disable Fire damage if pvp disabled and there's another getPlayer nearby
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

    private boolean isPvpEnabled() {
        return this.plugin.getSettings().isSettingEnabled(GameSetting.PVP);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvpDisabled(EntityDamageByEntityEvent event) {
        if (isPvpEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getDamager().getType() == EntityType.MINECART_TNT) {
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
