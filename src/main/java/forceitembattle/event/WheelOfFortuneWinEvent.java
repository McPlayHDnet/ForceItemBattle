package forceitembattle.event;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WheelOfFortuneWinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Material wonItem;

    public WheelOfFortuneWinEvent(Player player, Material wonItem) {
        this.player = player;
        this.wonItem = wonItem;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * The item the wheel landed on.
     */
    public Material getWonItem() {
        return wonItem;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}