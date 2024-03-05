package forceitembattle.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class FoundItemEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private Player player;
    private ItemStack foundItem;
    private boolean skipped, backToBack;

    public FoundItemEvent(Player player) {this.player = player;}

    public Player getPlayer() {
        return player;
    }

    public ItemStack getFoundItem() {
        return foundItem;
    }

    public void setFoundItem(ItemStack foundItem) {
        this.foundItem = foundItem;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setBackToBack(boolean backToBack) {
        this.backToBack = backToBack;
    }

    public boolean isBackToBack() { return backToBack; }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
