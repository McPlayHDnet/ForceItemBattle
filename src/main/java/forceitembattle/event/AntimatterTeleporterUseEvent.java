package forceitembattle.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AntimatterTeleporterUseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean newTeleporter;

    public AntimatterTeleporterUseEvent(Player player, boolean newTeleporter) {
        this.player = player;
        this.newTeleporter = newTeleporter;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isNewTeleporter() {
        return newTeleporter;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}