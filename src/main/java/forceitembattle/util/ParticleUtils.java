package forceitembattle.util;

import java.util.Objects;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ParticleUtils {

    private ParticleUtils() {
    }

    public static void drawLine(@NonNull Player player, @NonNull Location point1, @NonNull Location point2, @NonNull Particle particle, @Nullable Particle.DustOptions dustOptions, int count, double space, int max, double phase) {
        World world = point1.getWorld();
        if (!Objects.equals(world, point2.getWorld())) return;
        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector direction = point2.toVector().subtract(p1).normalize();
        Vector vector = direction.clone().multiply(space);
        p1.add(direction.multiply(phase));
        double length = phase;
        int current = 0;
        for (; length < distance; p1.add(vector)) {
            player.spawnParticle(particle, p1.getX(), p1.getY(), p1.getZ(), count, dustOptions);
            length += space;

            current++;
            if (current >= max) break;
        }

    }

}
