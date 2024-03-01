package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class PositionManager {

    private final ForceItemBattle forceItemBattle;
    private final Map<String, Location> positionsMap;

    public PositionManager(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.positionsMap = new HashMap<>();
    }

    public boolean positionExist(String positionName) {
        return this.positionsMap.containsKey(positionName);
    }

    public void createPosition(String positionName, Location location) {
        this.positionsMap.put(positionName, location);
    }

    public Location getPosition(String positionName) {
        return this.positionsMap.get(positionName);
    }

}
