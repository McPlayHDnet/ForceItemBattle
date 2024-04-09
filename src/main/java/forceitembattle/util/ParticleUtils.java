package forceitembattle.util;

import lombok.NonNull;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ParticleUtils {

    private ParticleUtils() {
    }

    private static void spawnParticleCircle(@NonNull Location location, int points, double radius, @NonNull BiConsumer<World, Location> player) {
        World world = location.getWorld();
        if (world == null) return;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location point = location.clone().add(radius * Math.sin(angle), 0.0d, radius * Math.cos(angle));
            player.accept(world, point);
        }
    }

    public static void spawnParticleCircle(@NonNull Location location, @NonNull Effect particle, int points, double radius) {
        spawnParticleCircle(location, points, radius, (world, point) -> world.playEffect(point, particle, 1));
    }

    public static void spawnParticleCircle(@NonNull Location location, @NonNull Particle particle, int points, double radius) {
        spawnParticleCircle(location, points, radius, (world, point) -> world.spawnParticle(particle, point, 1));
    }

    private static void spawnUpGoingParticleCircle(@NonNull JavaPlugin plugin, @NonNull Location location, int points, double radius, double height, @NonNull BiConsumer<World, Location> player) {
        for (double y = 0, i = 0; y < height; y += .25, i++) {
            final double Y = y;
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                spawnParticleCircle(location.clone().add(0, Y, 0), points, radius, player);
            }, (long) i);
        }
    }

    public static void spawnUpGoingParticleCircle(@NonNull JavaPlugin plugin, @NonNull Location location, @NonNull Effect particle, int points, double radius, double height) {
        spawnUpGoingParticleCircle(plugin, location, points, radius, height, (world, point) -> world.playEffect(point, particle, 1));
    }

    public static void spawnUpGoingParticleCircle(@NonNull JavaPlugin plugin, @NonNull Location location, @NonNull Particle particle, int points, double radius, double height) {
        spawnUpGoingParticleCircle(plugin, location, points, radius, height, (world, point) -> world.spawnParticle(particle, point, 1));
    }

    public static void spawnParticleCircleAroundEntity(@NonNull JavaPlugin plugin, @NonNull Entity entity) {
        spawnParticleCircleAroundBoundingBox(plugin, entity.getLocation(), Particle.SPELL_INSTANT, entity.getBoundingBox(), 0.25);
    }

    public static void spawnParticleCircleAroundBoundingBox(@NonNull JavaPlugin plugin, @NonNull Location location, @NonNull Particle particle, @NonNull BoundingBox box, double height) {
        spawnParticleCircleAroundRadius(plugin, location, particle, box.getWidthX(), height);
    }

    public static void spawnParticleCircleAroundRadius(@NonNull JavaPlugin plugin, @NonNull Location location, @NonNull Particle particle, double radius, double height) {
        spawnUpGoingParticleCircle(plugin, location, particle, (int) (radius * 15), radius, height);
    }

    public static void drawLine(@NonNull Player player, @NonNull Location point1, @NonNull Location point2, @NonNull Particle particle, @Nullable Particle.DustOptions dustOptions, int count, double space, int max) {
        World world = point1.getWorld();
        if (!Objects.equals(world, point2.getWorld())) return;
        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize().multiply(space);
        double length = 0;
        int current = 0;
        for (; length < distance; p1.add(vector)) {
            player.spawnParticle(particle, p1.getX(), p1.getY(), p1.getZ(), count, dustOptions);
            length += space;

            current++;
            if (current >= max) break;
        }

    }
    
}
