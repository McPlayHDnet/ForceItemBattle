package forceitembattle.ceremony;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Entity;

/** A real cushion, held in the air. */
public final class CushionSeats implements SeatSpawner {

    @Override
    public Entity spawn(Location at) {
        return at.getWorld().spawn(at, Cushion.class, cushion -> {
            cushion.setColor(DyeColor.RED);
            cushion.setGravity(false);
            cushion.setInvulnerable(true);
            cushion.setPersistent(false);
        });
    }
}
