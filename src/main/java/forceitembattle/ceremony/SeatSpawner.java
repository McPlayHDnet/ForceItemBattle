package forceitembattle.ceremony;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/** Spawns what one player sits on. A seam because the tests run on a Paper API that predates cushions. */
@FunctionalInterface
public interface SeatSpawner {

    Entity spawn(Location at);
}
