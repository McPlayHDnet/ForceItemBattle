package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import org.bukkit.Location;

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

}
