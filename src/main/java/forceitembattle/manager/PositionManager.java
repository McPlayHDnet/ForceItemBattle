package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ParticleUtils;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class PositionManager {

    private final ForceItemBattle plugin;
    private final Map<String, Location> positionsMap;

    public PositionManager(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.positionsMap = new HashMap<>();
    }

    public boolean positionExist(String positionName) {
        return this.positionsMap.containsKey(positionName.toLowerCase());
    }

    public void createPosition(String positionName, Location location) {
        this.positionsMap.put(positionName.toLowerCase(), location);
    }

    public void removePosition(String positionName) {
        this.positionsMap.remove(positionName.toLowerCase());
    }

    public Map<String, Location> getAllPositions() {
        return this.positionsMap;
    }

    public void clearPositions() {
        this.positionsMap.clear();
    }

    public Location getPosition(String positionName) {
        return this.positionsMap.get(positionName.toLowerCase());
    }

    public void playParticleLine(@NonNull Player player, @NonNull Location position, Color color) {
        if (player.getWorld() != position.getWorld()) return;

        // Defining target location to
        Location target = position.clone().add(0, 0.3, 0);

        new BukkitRunnable() {
            int current = 0;
            @Override
            public void run() {
                if(++current == 10) {
                    this.cancel();
                }
                ParticleUtils.drawLine(player, player.getLocation().add(0, 1.2, 0), target, Particle.DUST, new Particle.DustOptions(color, 1), 1, 0.5, 50);
            }
        }.runTaskTimer(this.plugin, 0L, 10L);
    }

}
